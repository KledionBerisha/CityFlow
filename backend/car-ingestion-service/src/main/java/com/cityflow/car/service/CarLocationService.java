package com.cityflow.car.service;

import com.cityflow.car.dto.CarLocationResponse;
import com.cityflow.car.model.CarLocation;
import com.cityflow.car.model.CarLocationCache;
import com.cityflow.car.repository.CarLocationCacheRepository;
import com.cityflow.car.repository.CarLocationRepository;
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

@Service
public class CarLocationService {
    
    private static final Logger log = LoggerFactory.getLogger(CarLocationService.class);

    private final CarLocationRepository locationRepository;
    private final CarLocationCacheRepository cacheRepository;

    public CarLocationService(
            CarLocationRepository locationRepository,
            CarLocationCacheRepository cacheRepository) {
        this.locationRepository = locationRepository;
        this.cacheRepository = cacheRepository;
    }

    /**
     * Get current (cached) location for a specific car
     */
    public Mono<CarLocationResponse> getCurrentLocation(String carId) {
        return cacheRepository.findByCarId(carId)
                .map(this::cacheToResponse)
                .switchIfEmpty(
                        // Fallback to database if not in cache
                        locationRepository.findFirstByCarIdOrderByTimestampDesc(carId)
                                .map(this::toResponse)
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No location found for car")));
    }

    /**
     * Get current locations for all active cars
     */
    public Flux<CarLocationResponse> getAllCurrentLocations() {
        return cacheRepository.findAll()
                .map(this::cacheToResponse);
    }

    /**
     * Get location history for a car
     */
    public Flux<CarLocationResponse> getLocationHistory(String carId, int hours, int limit) {
        Instant start = Instant.now().minus(hours, ChronoUnit.HOURS);
        Instant end = Instant.now();
        
        return locationRepository.findByCarIdAndTimestampBetweenOrderByTimestampDesc(
                        carId, start, end, PageRequest.of(0, limit))
                .map(this::toResponse);
    }

    /**
     * Get recent locations (for polling)
     */
    public Flux<CarLocationResponse> getRecentLocations(int secondsAgo) {
        Instant since = Instant.now().minus(secondsAgo, ChronoUnit.SECONDS);
        return locationRepository.findByTimestampAfterOrderByTimestampDesc(since)
                .map(this::toResponse);
    }

    /**
     * Get locations within a geographic bounding box (for traffic analysis)
     */
    public Flux<CarLocationResponse> getLocationsInArea(
            Double minLat, Double maxLat, Double minLon, Double maxLon) {
        Instant since = Instant.now().minus(30, ChronoUnit.SECONDS);
        return locationRepository.findByLatitudeBetweenAndLongitudeBetweenAndTimestampAfter(
                        minLat, maxLat, minLon, maxLon, since)
                .map(this::toResponse);
    }

    private CarLocationResponse toResponse(CarLocation location) {
        return CarLocationResponse.builder()
                .id(location.getId())
                .carId(location.getCarId())
                .vehicleId(location.getVehicleId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(location.getHeading())
                .timestamp(location.getTimestamp())
                .source(location.getSource() != null ? location.getSource().name() : null)
                .trafficDensity(location.getTrafficDensity())
                .congestionLevel(location.getCongestionLevel())
                .build();
    }

    private CarLocationResponse cacheToResponse(CarLocationCache cache) {
        return CarLocationResponse.builder()
                .carId(cache.getCarId())
                .vehicleId(cache.getVehicleId())
                .latitude(cache.getLatitude())
                .longitude(cache.getLongitude())
                .speedKmh(cache.getSpeedKmh())
                .heading(cache.getHeading())
                .timestamp(cache.getTimestamp())
                .trafficDensity(cache.getTrafficDensity())
                .congestionLevel(cache.getCongestionLevel())
                .build();
    }
}

