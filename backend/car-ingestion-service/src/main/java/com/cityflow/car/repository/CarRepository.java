package com.cityflow.car.repository;

import com.cityflow.car.model.Car;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CarRepository extends ReactiveMongoRepository<Car, String> {

    Mono<Car> findByVehicleId(String vehicleId);

    Mono<Boolean> existsByVehicleId(String vehicleId);

    Flux<Car> findByStatus(Car.CarStatus status);
}

