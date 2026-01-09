package com.cityflow.bus.repository;

import com.cityflow.bus.model.BusLocationCache;
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
import java.util.UUID;

@Repository
public class BusLocationCacheRepository {
    
    private static final Logger log = LoggerFactory.getLogger(BusLocationCacheRepository.class);

    private static final String CACHE_KEY_PREFIX = "bus:location:";
    private static final String ROUTE_KEY_PREFIX = "route:buses:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public BusLocationCacheRepository(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${app.redis.ttl-seconds:300}") int ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Save bus location to Redis cache
     */
    public Mono<Boolean> save(BusLocationCache location) {
        String key = CACHE_KEY_PREFIX + location.getBusId();
        try {
            String json = objectMapper.writeValueAsString(location);
            return redisTemplate.opsForValue()
                    .set(key, json, ttl)
                    .doOnSuccess(v -> log.debug("Cached location for bus: {}", location.getBusId()))
                    .doOnError(e -> log.error("Failed to cache location for bus: {}", location.getBusId(), e));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize bus location", e);
            return Mono.just(false);
        }
    }

    /**
     * Get cached location for a specific bus
     */
    public Mono<BusLocationCache> findByBusId(String busId) {
        String key = CACHE_KEY_PREFIX + busId;
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(json -> {
                    try {
                        BusLocationCache location = objectMapper.readValue(json, BusLocationCache.class);
                        return Mono.just(location);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize bus location", e);
                        return Mono.empty();
                    }
                });
    }

    /**
     * Get all cached bus locations
     */
    public Flux<BusLocationCache> findAll() {
        return redisTemplate.keys(CACHE_KEY_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().get(key))
                .flatMap(json -> {
                    try {
                        BusLocationCache location = objectMapper.readValue(json, BusLocationCache.class);
                        return Mono.just(location);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize bus location", e);
                        return Mono.empty();
                    }
                });
    }

    /**
     * Get all buses on a specific route (uses secondary index)
     */
    public Flux<BusLocationCache> findByRouteId(UUID routeId) {
        String routeKey = ROUTE_KEY_PREFIX + routeId;
        return redisTemplate.opsForSet()
                .members(routeKey)
                .flatMap(this::findByBusId);
    }

    /**
     * Index bus by route for faster route-based queries
     */
    public Mono<Long> addToRouteIndex(String busId, UUID routeId) {
        String routeKey = ROUTE_KEY_PREFIX + routeId;
        return redisTemplate.opsForSet()
                .add(routeKey, busId)
                .doOnSuccess(v -> log.debug("Added bus {} to route index {}", busId, routeId));
    }

    /**
     * Remove bus from route index
     */
    public Mono<Long> removeFromRouteIndex(String busId, UUID routeId) {
        String routeKey = ROUTE_KEY_PREFIX + routeId;
        return redisTemplate.opsForSet()
                .remove(routeKey, busId);
    }

    /**
     * Delete cached location
     */
    public Mono<Boolean> delete(String busId) {
        String key = CACHE_KEY_PREFIX + busId;
        return redisTemplate.delete(key)
                .map(count -> count > 0);
    }
}
