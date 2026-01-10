package com.cityflow.traffic.service;

import com.cityflow.traffic.dto.RoadTrafficDto;
import com.cityflow.traffic.event.TrafficReadingEvent;
import com.cityflow.traffic.model.CongestionLevel;
import com.cityflow.traffic.model.Sensor;
import com.cityflow.traffic.model.TrafficReading;
import com.cityflow.traffic.repository.TrafficReadingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class TrafficReadingService {

    private static final Logger log = LoggerFactory.getLogger(TrafficReadingService.class);
    private static final String REDIS_KEY_PREFIX = "traffic:current:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final TrafficReadingRepository trafficReadingRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SensorService sensorService;
    private final ObjectMapper objectMapper;

    public TrafficReadingService(
            TrafficReadingRepository trafficReadingRepository,
            KafkaProducerService kafkaProducerService,
            ReactiveRedisTemplate<String, String> redisTemplate,
            SensorService sensorService,
            ObjectMapper objectMapper) {
        this.trafficReadingRepository = trafficReadingRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.redisTemplate = redisTemplate;
        this.sensorService = sensorService;
        this.objectMapper = objectMapper;
    }

    public Mono<TrafficReading> recordReading(TrafficReading reading) {
        return trafficReadingRepository.save(reading)
                .flatMap(saved -> {
                    log.debug("Recorded traffic reading for sensor {}: speed={}, count={}, congestion={}",
                            saved.getSensorCode(), saved.getAverageSpeed(), 
                            saved.getVehicleCount(), saved.getCongestionLevel());

                    // Cache in Redis
                    return cacheReading(saved)
                            .then(Mono.just(saved));
                })
                .doOnSuccess(saved -> {
                    // Publish to Kafka
                    TrafficReadingEvent event = buildEvent(saved);
                    kafkaProducerService.publishTrafficReadingEvent(event);
                });
    }

    public Mono<TrafficReading> getCurrentReading(String sensorId) {
        String key = REDIS_KEY_PREFIX + sensorId;
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, TrafficReading.class));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize reading from cache", e);
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(
                        trafficReadingRepository.findBySensorIdOrderByTimestampDesc(
                                sensorId, PageRequest.of(0, 1))
                                .next()
                );
    }

    public Flux<TrafficReading> getAllCurrentReadings() {
        return redisTemplate.keys(REDIS_KEY_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, TrafficReading.class));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize reading from cache", e);
                        return Mono.empty();
                    }
                });
    }

    public Flux<TrafficReading> getReadingHistory(String sensorId, int limit) {
        return trafficReadingRepository.findBySensorIdOrderByTimestampDesc(
                sensorId, PageRequest.of(0, limit));
    }

    public Flux<TrafficReading> getReadingsByTimeRange(String sensorId, Instant start, Instant end) {
        return trafficReadingRepository.findBySensorIdAndTimestampBetweenOrderByTimestampDesc(
                sensorId, start, end);
    }

    public Flux<TrafficReading> getReadingsByRoadSegment(String roadSegmentId, int limit) {
        return trafficReadingRepository.findByRoadSegmentIdOrderByTimestampDesc(
                roadSegmentId, PageRequest.of(0, limit));
    }

    public Flux<TrafficReading> getRecentReadings(Duration duration) {
        Instant since = Instant.now().minus(duration);
        return trafficReadingRepository.findRecentReadings(since);
    }

    public Flux<TrafficReading> getReadingsByCongestionLevel(CongestionLevel level) {
        return trafficReadingRepository.findByCongestionLevel(level);
    }

    public Flux<TrafficReading> getIncidentReadings() {
        return trafficReadingRepository.findByIncidentDetectedTrue();
    }

    /**
     * Get road traffic data grouped by road segments with GeoJSON geometry
     * Only returns roads with valid geometry (at least 2 coordinates)
     */
    public Flux<RoadTrafficDto> getRoadTrafficData() {
        return getAllCurrentReadings()
                .collectList()
                .flatMapMany(readings -> {
                    // Group readings by road segment
                    Map<String, List<TrafficReading>> byRoadSegment = new HashMap<>();
                    for (TrafficReading reading : readings) {
                        String roadSegmentId = reading.getRoadSegmentId();
                        if (roadSegmentId != null) {
                            byRoadSegment.computeIfAbsent(roadSegmentId, k -> new ArrayList<>())
                                    .add(reading);
                        }
                    }

                    // Create RoadTrafficDto for each road segment, filter out those without valid geometry
                    return Flux.fromIterable(byRoadSegment.entrySet())
                            .flatMap(entry -> buildRoadTrafficDto(entry.getKey(), entry.getValue()))
                            .filter(dto -> dto.getGeometry() != null 
                                    && dto.getGeometry().getCoordinates() != null 
                                    && dto.getGeometry().getCoordinates().size() >= 2);
                });
    }

    private Mono<RoadTrafficDto> buildRoadTrafficDto(String roadSegmentId, List<TrafficReading> readings) {
        // Calculate aggregated metrics
        double avgSpeed = readings.stream()
                .filter(r -> r.getAverageSpeed() != null)
                .mapToDouble(TrafficReading::getAverageSpeed)
                .average()
                .orElse(0.0);

        int totalVehicles = readings.stream()
                .filter(r -> r.getVehicleCount() != null)
                .mapToInt(TrafficReading::getVehicleCount)
                .sum();

        // Determine worst congestion level
        CongestionLevel worstCongestion = readings.stream()
                .map(TrafficReading::getCongestionLevel)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(CongestionLevel.FREE_FLOW);

        // Get latest timestamp
        String lastUpdated = readings.stream()
                .map(TrafficReading::getTimestamp)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .map(Instant::toString)
                .orElse(Instant.now().toString());

        // Get sensor IDs to fetch locations
        List<String> sensorIds = readings.stream()
                .map(TrafficReading::getSensorId)
                .distinct()
                .toList();

        // Fetch sensor locations and build geometry
        return sensorService.getAllSensors()
                .filter(sensor -> sensorIds.contains(sensor.getId()))
                .collectList()
                .map(sensors -> {
                    RoadTrafficDto dto = new RoadTrafficDto(roadSegmentId, "Road " + roadSegmentId);
                    dto.setAverageSpeed(avgSpeed);
                    dto.setVehicleCount(totalVehicles);
                    dto.setCongestionLevel(RoadTrafficDto.mapCongestionLevel(worstCongestion));
                    dto.setLastUpdated(lastUpdated);

                    // Build LineString geometry from sensor locations
                    // Filter out sensors with null location or null coordinates
                    List<List<Double>> coordinates = sensors.stream()
                            .filter(s -> s.getLocation() != null 
                                    && s.getLocation().getLongitude() != null 
                                    && s.getLocation().getLatitude() != null)
                            .map(s -> Arrays.asList(
                                    s.getLocation().getLongitude(),
                                    s.getLocation().getLatitude()
                            ))
                            .toList();

                    // LineString requires at least 2 points to be valid GeoJSON
                    if (coordinates.size() >= 2) {
                        dto.setGeometry(new RoadTrafficDto.GeoJsonGeometry(coordinates));
                    }

                    return dto;
                });
    }

    private Mono<Boolean> cacheReading(TrafficReading reading) {
        String key = REDIS_KEY_PREFIX + reading.getSensorId();
        try {
            String json = objectMapper.writeValueAsString(reading);
            return redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize reading for cache", e);
            return Mono.just(false);
        }
    }

    private TrafficReadingEvent buildEvent(TrafficReading reading) {
        TrafficReadingEvent event = new TrafficReadingEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(reading.getTimestamp());
        event.setSensorId(reading.getSensorId());
        event.setSensorCode(reading.getSensorCode());
        event.setRoadSegmentId(reading.getRoadSegmentId());
        event.setAverageSpeed(reading.getAverageSpeed());
        event.setVehicleCount(reading.getVehicleCount());
        event.setOccupancy(reading.getOccupancy());
        event.setCongestionLevel(reading.getCongestionLevel());
        event.setQueueLength(reading.getQueueLength());
        event.setIncidentDetected(reading.getIncidentDetected());
        return event;
    }
}
