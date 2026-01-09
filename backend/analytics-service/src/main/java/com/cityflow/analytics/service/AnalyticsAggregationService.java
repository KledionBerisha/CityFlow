package com.cityflow.analytics.service;

import com.cityflow.analytics.dto.BusLocationEvent;
import com.cityflow.analytics.dto.TrafficReadingEvent;
import com.cityflow.analytics.model.CityWideMetrics;
import com.cityflow.analytics.model.RoadSegmentMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalyticsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationService.class);
    private static final String REDIS_ROAD_SEGMENT_PREFIX = "analytics:segment:";
    private static final String REDIS_CITY_METRICS_KEY = "analytics:city:metrics";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // In-memory aggregation buffers
    private final Map<String, List<TrafficReadingEvent>> trafficBuffer = new ConcurrentHashMap<>();
    private final Map<String, List<BusLocationEvent>> busBuffer = new ConcurrentHashMap<>();
    private final Map<String, Integer> incidentCounts = new ConcurrentHashMap<>();

    public AnalyticsAggregationService(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void processTrafficReading(TrafficReadingEvent event) {
        if (event.getRoadSegmentId() == null) {
            return;
        }

        // Add to buffer
        trafficBuffer.computeIfAbsent(event.getRoadSegmentId(), k -> new ArrayList<>()).add(event);

        // Track incidents
        if (Boolean.TRUE.equals(event.getIncidentDetected())) {
            incidentCounts.merge(event.getRoadSegmentId(), 1, Integer::sum);
        }

        // Aggregate and cache
        aggregateRoadSegmentMetrics(event.getRoadSegmentId());
        aggregateCityMetrics();
    }

    public void processBusLocation(BusLocationEvent event) {
        if (event.getRouteId() == null) {
            return;
        }

        // Add to buffer
        busBuffer.computeIfAbsent(event.getRouteId(), k -> new ArrayList<>()).add(event);

        // Aggregate city metrics
        aggregateCityMetrics();
    }

    private void aggregateRoadSegmentMetrics(String roadSegmentId) {
        List<TrafficReadingEvent> readings = trafficBuffer.get(roadSegmentId);
        if (readings == null || readings.isEmpty()) {
            return;
        }

        RoadSegmentMetrics metrics = new RoadSegmentMetrics(roadSegmentId);

        // Calculate averages
        double totalSpeed = 0;
        int totalVehicles = 0;
        double totalOccupancy = 0;
        int totalQueue = 0;
        int readingCount = readings.size();
        Map<String, Integer> congestionCounts = new HashMap<>();

        for (TrafficReadingEvent reading : readings) {
            if (reading.getAverageSpeed() != null) totalSpeed += reading.getAverageSpeed();
            if (reading.getVehicleCount() != null) totalVehicles += reading.getVehicleCount();
            if (reading.getOccupancy() != null) totalOccupancy += reading.getOccupancy();
            if (reading.getQueueLength() != null) totalQueue += reading.getQueueLength();
            if (reading.getCongestionLevel() != null) {
                congestionCounts.merge(reading.getCongestionLevel(), 1, Integer::sum);
            }
        }

        metrics.setAverageSpeed(totalSpeed / readingCount);
        metrics.setTotalVehicles(totalVehicles);
        metrics.setAverageOccupancy(totalOccupancy / readingCount);
        metrics.setQueueLength(totalQueue / readingCount);
        metrics.setIncidentCount(incidentCounts.getOrDefault(roadSegmentId, 0));

        // Most common congestion level
        String mostCommonCongestion = congestionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
        metrics.setCongestionLevel(mostCommonCongestion);

        // Calculate traffic flow score (0-100)
        double flowScore = calculateFlowScore(metrics.getAverageSpeed(), metrics.getAverageOccupancy(), mostCommonCongestion);
        metrics.setTrafficFlowScore(flowScore);

        // Count active buses on this segment (simplified - would need route-to-segment mapping)
        metrics.setActiveBusCount(0);
        metrics.setAverageBusDelay(0.0);

        // Cache in Redis
        cacheRoadSegmentMetrics(metrics);

        // Keep buffer size manageable
        if (readings.size() > 100) {
            trafficBuffer.put(roadSegmentId, readings.subList(readings.size() - 50, readings.size()));
        }
    }

    private void aggregateCityMetrics() {
        CityWideMetrics metrics = new CityWideMetrics();

        // Aggregate from all road segments
        int totalSensors = trafficBuffer.size();
        double totalSpeed = 0;
        int totalVehicles = 0;
        int speedCount = 0;
        Map<String, Integer> cityCongestionCounts = new HashMap<>();

        for (List<TrafficReadingEvent> readings : trafficBuffer.values()) {
            if (!readings.isEmpty()) {
                TrafficReadingEvent latest = readings.get(readings.size() - 1);
                if (latest.getAverageSpeed() != null) {
                    totalSpeed += latest.getAverageSpeed();
                    speedCount++;
                }
                if (latest.getVehicleCount() != null) {
                    totalVehicles += latest.getVehicleCount();
                }
                if (latest.getCongestionLevel() != null) {
                    cityCongestionCounts.merge(latest.getCongestionLevel(), 1, Integer::sum);
                }
            }
        }

        metrics.setTotalActiveSensors(totalSensors);
        metrics.setTotalRoadSegments(totalSensors);
        metrics.setCityAverageSpeed(speedCount > 0 ? totalSpeed / speedCount : 0.0);
        metrics.setTotalVehiclesDetected(totalVehicles);
        metrics.setCongestionLevelCounts(cityCongestionCounts);

        // Bus metrics
        int totalBuses = busBuffer.values().stream()
                .mapToInt(List::size)
                .sum();
        metrics.setTotalActiveBuses(totalBuses);
        metrics.setBusesOnTime((int) (totalBuses * 0.7));  // Simplified
        metrics.setBusesDelayed((int) (totalBuses * 0.3));
        metrics.setAverageDelay(2.5);

        // Incidents
        int totalIncidents = incidentCounts.values().stream().mapToInt(Integer::intValue).sum();
        metrics.setActiveIncidents(totalIncidents);

        // City traffic score
        double cityScore = calculateCityScore(metrics);
        metrics.setCityTrafficScore(cityScore);

        // Cache in Redis
        cacheCityMetrics(metrics);
    }

    private double calculateFlowScore(Double speed, Double occupancy, String congestionLevel) {
        if (speed == null || occupancy == null) return 50.0;

        double score = 100.0;

        // Speed factor (higher is better)
        if (speed < 20) score -= 40;
        else if (speed < 40) score -= 20;
        else if (speed > 80) score -= 10;

        // Occupancy factor (moderate is best)
        if (occupancy > 0.8) score -= 30;
        else if (occupancy > 0.6) score -= 15;
        else if (occupancy < 0.2) score += 10;

        // Congestion factor
        switch (congestionLevel) {
            case "SEVERE": score -= 40; break;
            case "HEAVY": score -= 25; break;
            case "MODERATE": score -= 10; break;
            case "LIGHT": score += 5; break;
            case "FREE_FLOW": score += 10; break;
        }

        return Math.max(0, Math.min(100, score));
    }

    private double calculateCityScore(CityWideMetrics metrics) {
        double score = 100.0;

        // Average speed factor
        if (metrics.getCityAverageSpeed() != null) {
            if (metrics.getCityAverageSpeed() < 30) score -= 30;
            else if (metrics.getCityAverageSpeed() < 45) score -= 15;
        }

        // Congestion distribution
        Map<String, Integer> congestion = metrics.getCongestionLevelCounts();
        int severe = congestion.getOrDefault("SEVERE", 0);
        int heavy = congestion.getOrDefault("HEAVY", 0);
        score -= (severe * 5 + heavy * 3);

        // Incident factor
        score -= (metrics.getActiveIncidents() * 2);

        // Bus performance
        if (metrics.getTotalActiveBuses() != null && metrics.getTotalActiveBuses() > 0) {
            double onTimeRate = (double) metrics.getBusesOnTime() / metrics.getTotalActiveBuses();
            score += (onTimeRate * 10 - 5);
        }

        return Math.max(0, Math.min(100, score));
    }

    private void cacheRoadSegmentMetrics(RoadSegmentMetrics metrics) {
        try {
            String key = REDIS_ROAD_SEGMENT_PREFIX + metrics.getRoadSegmentId();
            String json = objectMapper.writeValueAsString(metrics);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL).subscribe(
                    success -> log.debug("Cached metrics for segment: {}", metrics.getRoadSegmentId()),
                    error -> log.error("Failed to cache segment metrics", error)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize segment metrics", e);
        }
    }

    private void cacheCityMetrics(CityWideMetrics metrics) {
        try {
            String json = objectMapper.writeValueAsString(metrics);
            redisTemplate.opsForValue().set(REDIS_CITY_METRICS_KEY, json, CACHE_TTL).subscribe(
                    success -> log.debug("Cached city-wide metrics"),
                    error -> log.error("Failed to cache city metrics", error)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize city metrics", e);
        }
    }

    public Mono<RoadSegmentMetrics> getRoadSegmentMetrics(String roadSegmentId) {
        String key = REDIS_ROAD_SEGMENT_PREFIX + roadSegmentId;
        return redisTemplate.opsForValue().get(key)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RoadSegmentMetrics.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize segment metrics", e);
                        return null;
                    }
                });
    }

    public Mono<CityWideMetrics> getCityMetrics() {
        return redisTemplate.opsForValue().get(REDIS_CITY_METRICS_KEY)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, CityWideMetrics.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize city metrics", e);
                        return null;
                    }
                });
    }

    public Mono<List<RoadSegmentMetrics>> getAllRoadSegmentMetrics() {
        return redisTemplate.keys(REDIS_ROAD_SEGMENT_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .mapNotNull(json -> {
                    try {
                        return objectMapper.readValue(json, RoadSegmentMetrics.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .collectList();
    }
}
