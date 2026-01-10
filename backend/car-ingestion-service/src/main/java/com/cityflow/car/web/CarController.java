package com.cityflow.car.web;

import com.cityflow.car.dto.CarRequest;
import com.cityflow.car.dto.CarResponse;
import com.cityflow.car.service.CarService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/cars")
public class CarController {
    
    private static final Logger log = LoggerFactory.getLogger(CarController.class);

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CarResponse> create(@Valid @RequestBody CarRequest request) {
        log.info("Creating car: {}", request.getVehicleId());
        return carService.create(request);
    }

    @GetMapping
    public Flux<CarResponse> findAll() {
        return carService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<CarResponse> findById(@PathVariable String id) {
        return carService.findById(id);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public Mono<CarResponse> findByVehicleId(@PathVariable String vehicleId) {
        return carService.findByVehicleId(vehicleId);
    }

    @GetMapping("/status/{status}")
    public Flux<CarResponse> findByStatus(@PathVariable String status) {
        return carService.findByStatus(status);
    }

    @PatchMapping("/{id}/status")
    public Mono<CarResponse> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        log.info("Updating car {} status to {}", id, status);
        return carService.updateStatus(id, status);
    }
}

