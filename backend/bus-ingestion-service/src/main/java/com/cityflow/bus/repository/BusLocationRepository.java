package com.cityflow.bus.repository;

import com.cityflow.bus.model.BusLocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface BusLocationRepository extends ReactiveMongoRepository<BusLocation, String> {

    /**
     * Get the most recent location for a specific bus
     */
    Mono<BusLocation> findFirstByBusIdOrderByTimestampDesc(String busId);

    /**
     * Get location history for a bus within a time range
     */
    Flux<BusLocation> findByBusIdAndTimestampBetweenOrderByTimestampDesc(
            String busId, Instant start, Instant end, Pageable pageable);

    /**
     * Get recent locations for all buses on a route
     */
    Flux<BusLocation> findByRouteIdAndTimestampAfterOrderByTimestampDesc(
            UUID routeId, Instant after);

    /**
     * Get all locations after a certain timestamp (for polling)
     */
    Flux<BusLocation> findByTimestampAfterOrderByTimestampDesc(Instant after);

    /**
     * Delete old location records (for data retention)
     */
    Mono<Long> deleteByTimestampBefore(Instant before);
}
