package com.cityflow.bus.web;

import com.cityflow.bus.dto.BusRequest;
import com.cityflow.bus.dto.BusResponse;
import com.cityflow.bus.service.BusService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/buses")
public class BusController {
    
    private static final Logger log = LoggerFactory.getLogger(BusController.class);

    private final BusService busService;

    public BusController(BusService busService) {
        this.busService = busService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BusResponse> create(@Valid @RequestBody BusRequest request) {
        log.info("Creating bus: {}", request.getVehicleId());
        return busService.create(request);
    }

    @GetMapping
    public Flux<BusResponse> findAll() {
        return busService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<BusResponse> findById(@PathVariable String id) {
        return busService.findById(id);
    }

    @GetMapping("/vehicle/{vehicleId}")
    public Mono<BusResponse> findByVehicleId(@PathVariable String vehicleId) {
        return busService.findByVehicleId(vehicleId);
    }

    @GetMapping("/route/{routeId}")
    public Flux<BusResponse> findByRouteId(@PathVariable UUID routeId) {
        return busService.findByRouteId(routeId);
    }

    @PatchMapping("/{id}/status")
    public Mono<BusResponse> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        log.info("Updating bus {} status to {}", id, status);
        return busService.updateStatus(id, status);
    }

    @PutMapping("/{id}/route")
    public Mono<BusResponse> updateRoute(
            @PathVariable String id,
            @RequestParam(required = false) UUID routeId) {
        log.info("Updating bus {} route to {}", id, routeId);
        return busService.updateRoute(id, routeId);
    }
}
