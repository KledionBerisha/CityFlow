package com.cityflow.bus.web;

import com.cityflow.bus.dto.BusLocationResponse;
import com.cityflow.bus.service.BusLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/bus-locations")
public class BusLocationController {
    
    private static final Logger log = LoggerFactory.getLogger(BusLocationController.class);

    private final BusLocationService locationService;

    public BusLocationController(BusLocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * GET /bus-locations/current - Get all current bus locations
     */
    @GetMapping("/current")
    public Flux<BusLocationResponse> getAllCurrentLocations() {
        return locationService.getAllCurrentLocations();
    }

    /**
     * GET /bus-locations/current/{busId} - Get current location for a specific bus
     */
    @GetMapping("/current/{busId}")
    public Mono<BusLocationResponse> getCurrentLocation(@PathVariable String busId) {
        return locationService.getCurrentLocation(busId);
    }

    /**
     * GET /bus-locations/current/route/{routeId} - Get current locations for buses on a route
     */
    @GetMapping("/current/route/{routeId}")
    public Flux<BusLocationResponse> getCurrentLocationsByRoute(@PathVariable UUID routeId) {
        return locationService.getCurrentLocationsByRoute(routeId);
    }

    /**
     * GET /bus-locations/history/{busId} - Get location history for a bus
     */
    @GetMapping("/history/{busId}")
    public Flux<BusLocationResponse> getLocationHistory(
            @PathVariable String busId,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        return locationService.getLocationHistory(busId, hours, limit);
    }

    /**
     * GET /bus-locations/recent - Get recent location updates (for polling)
     */
    @GetMapping("/recent")
    public Flux<BusLocationResponse> getRecentLocations(
            @RequestParam(defaultValue = "30") int seconds) {
        return locationService.getRecentLocations(seconds);
    }
}
