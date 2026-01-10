package com.cityflow.car.web;

import com.cityflow.car.dto.CarLocationResponse;
import com.cityflow.car.service.CarLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/car-locations")
public class CarLocationController {
    
    private static final Logger log = LoggerFactory.getLogger(CarLocationController.class);

    private final CarLocationService locationService;

    public CarLocationController(CarLocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * GET /car-locations/current - Get all current car locations
     */
    @GetMapping("/current")
    public Flux<CarLocationResponse> getAllCurrentLocations() {
        return locationService.getAllCurrentLocations();
    }

    /**
     * GET /car-locations/current/{carId} - Get current location for a specific car
     */
    @GetMapping("/current/{carId}")
    public Mono<CarLocationResponse> getCurrentLocation(@PathVariable String carId) {
        return locationService.getCurrentLocation(carId);
    }

    /**
     * GET /car-locations/history/{carId} - Get location history for a car
     */
    @GetMapping("/history/{carId}")
    public Flux<CarLocationResponse> getLocationHistory(
            @PathVariable String carId,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        return locationService.getLocationHistory(carId, hours, limit);
    }

    /**
     * GET /car-locations/recent - Get recent location updates (for polling)
     */
    @GetMapping("/recent")
    public Flux<CarLocationResponse> getRecentLocations(
            @RequestParam(defaultValue = "30") int seconds) {
        return locationService.getRecentLocations(seconds);
    }

    /**
     * GET /car-locations/area - Get locations within a geographic area (for traffic analysis)
     */
    @GetMapping("/area")
    public Flux<CarLocationResponse> getLocationsInArea(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLon,
            @RequestParam double maxLon) {
        return locationService.getLocationsInArea(minLat, maxLat, minLon, maxLon);
    }

    /**
     * GET /car-locations/stream - Server-Sent Events stream for real-time car location updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CarLocationResponse>> streamLocations(
            @RequestParam(defaultValue = "3") int interval) {
        
        log.debug("Starting SSE stream for cars, interval: {}s", interval);
        
        return Flux.interval(Duration.ofSeconds(interval))
                .flatMap(tick -> locationService.getAllCurrentLocations())
                .map(location -> ServerSentEvent.<CarLocationResponse>builder()
                        .id(location.getCarId())
                        .event("location-update")
                        .data(location)
                        .build())
                .doOnError(e -> log.error("Error in SSE stream", e))
                .onErrorResume(e -> {
                    log.error("SSE stream error, sending error event", e);
                    return Flux.just(ServerSentEvent.<CarLocationResponse>builder()
                            .event("error")
                            .data(null)
                            .build());
                });
    }
}

