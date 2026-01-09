package com.cityflow.bus.service;

import com.cityflow.bus.dto.BusRequest;
import com.cityflow.bus.dto.BusResponse;
import com.cityflow.bus.event.BusStatusEvent;
import com.cityflow.bus.model.Bus;
import com.cityflow.bus.repository.BusRepository;
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
public class BusService {
    
    private static final Logger log = LoggerFactory.getLogger(BusService.class);

    private final BusRepository busRepository;
    private final KafkaProducerService kafkaProducerService;

    public BusService(BusRepository busRepository, KafkaProducerService kafkaProducerService) {
        this.busRepository = busRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Mono<BusResponse> create(BusRequest request) {
        return busRepository.existsByVehicleId(request.getVehicleId())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Vehicle ID already exists"));
                    }

                    Bus bus = Bus.builder()
                            .vehicleId(request.getVehicleId())
                            .licensePlate(request.getLicensePlate())
                            .currentRouteId(request.getCurrentRouteId())
                            .status(Bus.BusStatus.valueOf(request.getStatus()))
                            .capacity(request.getCapacity())
                            .model(request.getModel())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return busRepository.save(bus)
                            .map(this::toResponse)
                            .doOnSuccess(response -> log.info("Created bus: {}", response.getVehicleId()));
                });
    }

    public Flux<BusResponse> findAll() {
        return busRepository.findAll()
                .map(this::toResponse);
    }

    public Mono<BusResponse> findById(String id) {
        return busRepository.findById(id)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bus not found")));
    }

    public Mono<BusResponse> findByVehicleId(String vehicleId) {
        return busRepository.findByVehicleId(vehicleId)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bus not found")));
    }

    public Flux<BusResponse> findByRouteId(UUID routeId) {
        return busRepository.findByCurrentRouteId(routeId)
                .map(this::toResponse);
    }

    public Mono<BusResponse> updateStatus(String id, String newStatus) {
        return busRepository.findById(id)
                .flatMap(bus -> {
                    Bus.BusStatus oldStatus = bus.getStatus();
                    bus.setStatus(Bus.BusStatus.valueOf(newStatus));
                    bus.setUpdatedAt(Instant.now());

                    return busRepository.save(bus)
                            .doOnSuccess(updated -> {
                                // Publish status change event
                                BusStatusEvent event = BusStatusEvent.builder()
                                        .eventId(UUID.randomUUID().toString())
                                        .busId(bus.getId())
                                        .vehicleId(bus.getVehicleId())
                                        .previousStatus(oldStatus.name())
                                        .newStatus(newStatus)
                                        .routeId(bus.getCurrentRouteId())
                                        .timestamp(Instant.now())
                                        .build();
                                kafkaProducerService.publishStatusEvent(event);
                            })
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bus not found")));
    }

    public Mono<BusResponse> updateRoute(String id, UUID routeId) {
        return busRepository.findById(id)
                .flatMap(bus -> {
                    bus.setCurrentRouteId(routeId);
                    bus.setUpdatedAt(Instant.now());

                    return busRepository.save(bus)
                            .doOnSuccess(updated -> log.info("Updated bus {} route to {}", 
                                    bus.getVehicleId(), routeId))
                            .map(this::toResponse);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bus not found")));
    }

    private BusResponse toResponse(Bus bus) {
        return BusResponse.builder()
                .id(bus.getId())
                .vehicleId(bus.getVehicleId())
                .licensePlate(bus.getLicensePlate())
                .currentRouteId(bus.getCurrentRouteId())
                .status(bus.getStatus().name())
                .capacity(bus.getCapacity())
                .model(bus.getModel())
                .createdAt(bus.getCreatedAt())
                .updatedAt(bus.getUpdatedAt())
                .build();
    }
}
