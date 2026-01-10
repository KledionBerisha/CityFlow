package com.cityflow.car.repository;

import com.cityflow.car.model.CarLocationCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
public class CarLocationCacheRepository {
    
    private static final Logger log = LoggerFactory.getLogger(CarLocationCacheRepository.class);

    private static final String CACHE_KEY_PREFIX = "car:location:";
    private static final String GEO_INDEX_KEY = "car:geoindex";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public CarLocationCacheRepository(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${app.redis.ttl-seconds:300}") int ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Save car location to Redis cache
     */
    public Mono<Boolean> save(CarLocationCache location) {
        String key = CACHE_KEY_PREFIX + location.getCarId();
        try {
            String json = objectMapper.writeValueAsString(location);
            return redisTemplate.opsForValue()
                    .set(key, json, ttl)
                    .doOnSuccess(v -> log.debug("Cached location for car: {}", location.getCarId()))
                    .doOnError(e -> log.error("Failed to cache location for car: {}", location.getCarId(), e));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize car location", e);
            return Mono.just(false);
        }
    }

    /**
     * Get cached location for a specific car
     */
    public Mono<CarLocationCache> findByCarId(String carId) {
        String key = CACHE_KEY_PREFIX + carId;
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(json -> {
                    try {
                        CarLocationCache location = objectMapper.readValue(json, CarLocationCache.class);
                        return Mono.just(location);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize car location", e);
                        return Mono.empty();
                    }
                });
    }

    /**
     * Get all cached car locations
     */
    public Flux<CarLocationCache> findAll() {
        return redisTemplate.keys(CACHE_KEY_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .flatMap(json -> {
                    try {
                        CarLocationCache location = objectMapper.readValue(json, CarLocationCache.class);
                        return Mono.just(location);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize car location", e);
                        return Mono.empty();
                    }
                });
    }

    /**
     * Delete cached location
     */
    public Mono<Boolean> delete(String carId) {
        String key = CACHE_KEY_PREFIX + carId;
        return redisTemplate.delete(key)
                .map(count -> count > 0);
    }
}

