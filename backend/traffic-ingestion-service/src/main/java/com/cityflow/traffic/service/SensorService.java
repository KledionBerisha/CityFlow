package com.cityflow.traffic.service;

import com.cityflow.traffic.event.SensorStatusEvent;
import com.cityflow.traffic.model.Sensor;
import com.cityflow.traffic.model.SensorStatus;
import com.cityflow.traffic.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    private final SensorRepository sensorRepository;
    private final KafkaProducerService kafkaProducerService;

    public SensorService(SensorRepository sensorRepository, KafkaProducerService kafkaProducerService) {
        this.sensorRepository = sensorRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Mono<Sensor> createSensor(Sensor sensor) {
        return sensorRepository.existsByCode(sensor.getCode())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Sensor with code " + sensor.getCode() + " already exists"));
                    }
                    return sensorRepository.save(sensor)
                            .doOnSuccess(s -> log.info("Created sensor: {} ({})", s.getName(), s.getCode()));
                });
    }

    public Flux<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    public Mono<Sensor> getSensorById(String id) {
        return sensorRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sensor not found: " + id)));
    }

    public Mono<Sensor> getSensorByCode(String code) {
        return sensorRepository.findByCode(code)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sensor not found: " + code)));
    }

    public Flux<Sensor> getSensorsByStatus(SensorStatus status) {
        return sensorRepository.findByStatus(status);
    }

    public Flux<Sensor> getSensorsByRoadSegment(String roadSegmentId) {
        return sensorRepository.findByRoadSegmentId(roadSegmentId);
    }

    public Mono<Sensor> updateSensorStatus(String id, SensorStatus newStatus) {
        return sensorRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sensor not found: " + id)))
                .flatMap(sensor -> {
                    SensorStatus oldStatus = sensor.getStatus();
                    sensor.setStatus(newStatus);
                    return sensorRepository.save(sensor)
                            .doOnSuccess(s -> {
                                log.info("Updated sensor {} status: {} -> {}", s.getCode(), oldStatus, newStatus);
                                
                                // Publish status change event
                                SensorStatusEvent event = new SensorStatusEvent(
                                        s.getId(), s.getCode(), oldStatus, newStatus);
                                event.setEventId(UUID.randomUUID().toString());
                                kafkaProducerService.publishSensorStatusEvent(event);
                            });
                });
    }

    public Mono<Void> deleteSensor(String id) {
        return sensorRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Sensor not found: " + id)))
                .flatMap(sensor -> sensorRepository.delete(sensor)
                        .doOnSuccess(v -> log.info("Deleted sensor: {}", sensor.getCode())));
    }
}
