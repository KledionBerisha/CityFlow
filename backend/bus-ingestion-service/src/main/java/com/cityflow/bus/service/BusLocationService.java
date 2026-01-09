package com.cityflow.bus.service;

import com.cityflow.bus.dto.BusLocationResponse;
import com.cityflow.bus.model.BusLocation;
import com.cityflow.bus.model.BusLocationCache;
import com.cityflow.bus.repository.BusLocationCacheRepository;
import com.cityflow.bus.repository.BusLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class BusLocationService {
    
    private static final Logger log = LoggerFactory.getLogger(BusLocationService.class);

    private final BusLocationRepository locationRepository;
    private final BusLocationCacheRepository cacheRepository;

    public BusLocationService(
            BusLocationRepository locationRepository,
            BusLocationCacheRepository cacheRepository) {
        this.locationRepository = locationRepository;
        this.cacheRepository = cacheRepository;
    }

    /**
     * Get current (cached) location for a specific bus
     */
    public Mono<BusLocationResponse> getCurrentLocation(String busId) {
        return cacheRepository.findByBusId(busId)
                .map(this::cacheToResponse)
                .switchIfEmpty(
                        // Fallback to database if not in cache
                        locationRepository.findFirstByBusIdOrderByTimestampDesc(busId)
                                .map(this::toResponse)
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No location found for bus")));
    }

    /**
     * Get current locations for all active buses
     */
    public Flux<BusLocationResponse> getAllCurrentLocations() {
        return cacheRepository.findAll()
                .map(this::cacheToResponse);
    }

    /**
     * Get current locations for all buses on a specific route
     */
    public Flux<BusLocationResponse> getCurrentLocationsByRoute(UUID routeId) {
        return cacheRepository.findByRouteId(routeId)
                .map(this::cacheToResponse);
    }

    /**
     * Get location history for a bus
     */
    public Flux<BusLocationResponse> getLocationHistory(String busId, int hours, int limit) {
        Instant start = Instant.now().minus(hours, ChronoUnit.HOURS);
        Instant end = Instant.now();
        
        return locationRepository.findByBusIdAndTimestampBetweenOrderByTimestampDesc(
                        busId, start, end, PageRequest.of(0, limit))
                .map(this::toResponse);
    }

    /**
     * Get recent locations (for polling/real-time updates)
     */
    public Flux<BusLocationResponse> getRecentLocations(int secondsAgo) {
        Instant since = Instant.now().minus(secondsAgo, ChronoUnit.SECONDS);
        return locationRepository.findByTimestampAfterOrderByTimestampDesc(since)
                .map(this::toResponse);
    }

    private BusLocationResponse toResponse(BusLocation location) {
        return BusLocationResponse.builder()
                .id(location.getId())
                .busId(location.getBusId())
                .vehicleId(location.getVehicleId())
                .routeId(location.getRouteId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(location.getHeading())
                .timestamp(location.getTimestamp())
                .occupancy(location.getOccupancy())
                .source(location.getSource() != null ? location.getSource().name() : null)
                .nextStopId(location.getNextStopId())
                .distanceToNextStopKm(location.getDistanceToNextStopKm())
                .estimatedArrivalSeconds(location.getEstimatedArrivalSeconds())
                .build();
    }

    private BusLocationResponse cacheToResponse(BusLocationCache cache) {
        return BusLocationResponse.builder()
                .busId(cache.getBusId())
                .vehicleId(cache.getVehicleId())
                .routeId(cache.getRouteId())
                .latitude(cache.getLatitude())
                .longitude(cache.getLongitude())
                .speedKmh(cache.getSpeedKmh())
                .heading(cache.getHeading())
                .timestamp(cache.getTimestamp())
                .nextStopId(cache.getNextStopId())
                .distanceToNextStopKm(cache.getDistanceToNextStopKm())
                .estimatedArrivalSeconds(cache.getEstimatedArrivalSeconds())
                .build();
    }
}
