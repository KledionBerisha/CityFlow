package com.cityflow.traffic.repository;

import com.cityflow.traffic.model.Sensor;
import com.cityflow.traffic.model.SensorStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SensorRepository extends ReactiveMongoRepository<Sensor, String> {

    Mono<Sensor> findByCode(String code);

    Flux<Sensor> findByStatus(SensorStatus status);

    Flux<Sensor> findByRoadSegmentId(String roadSegmentId);

    Mono<Boolean> existsByCode(String code);
}
