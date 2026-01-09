package com.cityflow.traffic.web;

import com.cityflow.traffic.dto.SensorDto;
import com.cityflow.traffic.dto.UpdateSensorStatusRequest;
import com.cityflow.traffic.model.SensorStatus;
import com.cityflow.traffic.service.SensorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/sensors")
public class SensorController {

    private static final Logger log = LoggerFactory.getLogger(SensorController.class);

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SensorDto> createSensor(@Valid @RequestBody SensorDto sensorDto) {
        log.info("Creating sensor: {}", sensorDto.getCode());
        return sensorService.createSensor(sensorDto.toEntity())
                .map(SensorDto::fromEntity);
    }

    @GetMapping
    public Flux<SensorDto> getAllSensors() {
        return sensorService.getAllSensors()
                .map(SensorDto::fromEntity);
    }

    @GetMapping("/{id}")
    public Mono<SensorDto> getSensorById(@PathVariable String id) {
        return sensorService.getSensorById(id)
                .map(SensorDto::fromEntity);
    }

    @GetMapping("/code/{code}")
    public Mono<SensorDto> getSensorByCode(@PathVariable String code) {
        return sensorService.getSensorByCode(code)
                .map(SensorDto::fromEntity);
    }

    @GetMapping("/status/{status}")
    public Flux<SensorDto> getSensorsByStatus(@PathVariable SensorStatus status) {
        return sensorService.getSensorsByStatus(status)
                .map(SensorDto::fromEntity);
    }

    @GetMapping("/road-segment/{roadSegmentId}")
    public Flux<SensorDto> getSensorsByRoadSegment(@PathVariable String roadSegmentId) {
        return sensorService.getSensorsByRoadSegment(roadSegmentId)
                .map(SensorDto::fromEntity);
    }

    @PatchMapping("/{id}/status")
    public Mono<SensorDto> updateSensorStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateSensorStatusRequest request) {
        log.info("Updating sensor {} status to {}", id, request.getStatus());
        return sensorService.updateSensorStatus(id, request.getStatus())
                .map(SensorDto::fromEntity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSensor(@PathVariable String id) {
        log.info("Deleting sensor: {}", id);
        return sensorService.deleteSensor(id);
    }
}
