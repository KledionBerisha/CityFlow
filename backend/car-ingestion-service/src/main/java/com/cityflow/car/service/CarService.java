package com.cityflow.car.service;

import com.cityflow.car.dto.CarRequest;
import com.cityflow.car.dto.CarResponse;
import com.cityflow.car.event.CarStatusEvent;
import com.cityflow.car.model.Car;
import com.cityflow.car.repository.CarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class CarService {
    
    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final KafkaProducerService kafkaProducerService;

    public CarService(CarRepository carRepository, KafkaProducerService kafkaProducerService) {
        this.carRepository = carRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Mono<CarResponse> create(CarRequest request) {
        return carRepository.existsByVehicleId(request.getVehicleId())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Vehicle ID already exists"));
                    }

                    Car car = Car.builder()
                            .vehicleId(request.getVehicleId())
                            .licensePlate(request.getLicensePlate())
                            .status(Car.CarStatus.valueOf(request.getStatus()))
                            .make(request.getMake())
                            .model(request.getModel())
                            .color(request.getColor())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return carRepository.save(car)
                            .map(this::toResponse)
                            .doOnSuccess(response -> log.info("Created car: {}", response.getVehicleId()));
                });
    }

    public Flux<CarResponse> findAll() {
        return carRepository.findAll()
                .map(this::toResponse);
    }

    public Mono<CarResponse> findById(String id) {
        return carRepository.findById(id)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Car not found")));
    }

    public Mono<CarResponse> findByVehicleId(String vehicleId) {
        return carRepository.findByVehicleId(vehicleId)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Car not found")));
    }

    public Flux<CarResponse> findByStatus(String status) {
        return carRepository.findByStatus(Car.CarStatus.valueOf(status))
                .map(this::toResponse);
    }

    public Mono<CarResponse> updateStatus(String id, String newStatus) {
        return carRepository.findById(id)
                .flatMap(car -> {
                    Car.CarStatus oldStatus = car.getStatus();
                    car.setStatus(Car.CarStatus.valueOf(newStatus));
                    car.setUpdatedAt(Instant.now());

                    return carRepository.save(car)
                            .doOnSuccess(updated -> {
                                // Publish status change event
                                CarStatusEvent event = CarStatusEvent.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .carId(car.getId())
                                        .vehicleId(car.getVehicleId())
                                        .previousStatus(oldStatus.name())
                                        .newStatus(newStatus)
                                        .timestamp(Instant.now())
                                        .build();
                                kafkaProducerService.publishStatusEvent(event);
                            })
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Car not found")));
    }

    private CarResponse toResponse(Car car) {
        return CarResponse.builder()
                .id(car.getId())
                .vehicleId(car.getVehicleId())
                .licensePlate(car.getLicensePlate())
                .status(car.getStatus().name())
                .make(car.getMake())
                .model(car.getModel())
                .color(car.getColor())
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }
}

