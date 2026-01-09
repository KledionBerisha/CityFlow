package com.cityflow.traffic.repository;

import com.cityflow.traffic.model.TrafficReading;
import com.cityflow.traffic.model.CongestionLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface TrafficReadingRepository extends ReactiveMongoRepository<TrafficReading, String> {

    Flux<TrafficReading> findBySensorIdOrderByTimestampDesc(String sensorId, Pageable pageable);

    Flux<TrafficReading> findByRoadSegmentIdOrderByTimestampDesc(String roadSegmentId, Pageable pageable);

    Flux<TrafficReading> findBySensorIdAndTimestampBetweenOrderByTimestampDesc(
            String sensorId, Instant start, Instant end);

    Flux<TrafficReading> findByRoadSegmentIdAndTimestampBetweenOrderByTimestampDesc(
            String roadSegmentId, Instant start, Instant end);

    @Query("{ 'timestamp': { $gte: ?0 } }")
    Flux<TrafficReading> findRecentReadings(Instant since);

    Flux<TrafficReading> findByCongestionLevel(CongestionLevel level);

    Flux<TrafficReading> findByIncidentDetectedTrue();

    Mono<Long> countBySensorIdAndTimestampBetween(String sensorId, Instant start, Instant end);
}
