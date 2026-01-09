package com.cityflow.bus.repository;

import com.cityflow.bus.model.Bus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BusRepository extends ReactiveMongoRepository<Bus, String> {

    Mono<Bus> findByVehicleId(String vehicleId);

    Mono<Boolean> existsByVehicleId(String vehicleId);

    Flux<Bus> findByCurrentRouteId(UUID routeId);

    Flux<Bus> findByStatus(Bus.BusStatus status);
}
