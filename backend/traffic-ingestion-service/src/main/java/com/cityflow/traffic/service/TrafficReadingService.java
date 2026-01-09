package com.cityflow.traffic.service;

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
import java.util.UUID;

@Service
public class TrafficReadingService {

    private static final Logger log = LoggerFactory.getLogger(TrafficReadingService.class);
    private static final String REDIS_KEY_PREFIX = "traffic:current:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final TrafficReadingRepository trafficReadingRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public TrafficReadingService(
            TrafficReadingRepository trafficReadingRepository,
            KafkaProducerService kafkaProducerService,
            ReactiveRedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.trafficReadingRepository = trafficReadingRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.redisTemplate = redisTemplate;
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
