package com.cityflow.car.repository;

import com.cityflow.car.model.CarLocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface CarLocationRepository extends ReactiveMongoRepository<CarLocation, String> {

    /**
     * Get the most recent location for a specific car
     */
    Mono<CarLocation> findFirstByCarIdOrderByTimestampDesc(String carId);

    /**
     * Get location history for a car within a time range
     */
    Flux<CarLocation> findByCarIdAndTimestampBetweenOrderByTimestampDesc(
            String carId, Instant start, Instant end, Pageable pageable);

    /**
     * Get all locations after a certain timestamp (for polling)
     */
    Flux<CarLocation> findByTimestampAfterOrderByTimestampDesc(Instant after);

    /**
     * Get locations within a geographic bounding box (for traffic density calculations)
     */
    Flux<CarLocation> findByLatitudeBetweenAndLongitudeBetweenAndTimestampAfter(
            Double minLat, Double maxLat, Double minLon, Double maxLon, Instant after);

    /**
     * Delete old location records (for data retention)
     */
    Mono<Long> deleteByTimestampBefore(Instant before);
}

