package com.cityflow.bus.simulator;

import com.cityflow.bus.event.BusLocationEvent;
import com.cityflow.bus.model.Bus;
import com.cityflow.bus.model.BusLocation;
import com.cityflow.bus.model.BusLocationCache;
import com.cityflow.bus.repository.BusLocationCacheRepository;
import com.cityflow.bus.repository.BusLocationRepository;
import com.cityflow.bus.repository.BusRepository;
import com.cityflow.bus.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GpsSimulator {
    
    private static final Logger log = LoggerFactory.getLogger(GpsSimulator.class);

    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final BusLocationCacheRepository cacheRepository;
    private final KafkaProducerService kafkaProducerService;
    private final boolean simulatorEnabled;
    private final double speedKmh;
    private final int intervalMs;

    // Store current position state for each bus
    private final ConcurrentHashMap<String, SimulatedBusState> busStates = new ConcurrentHashMap<>();

    public GpsSimulator(
            BusRepository busRepository,
            BusLocationRepository busLocationRepository,
            BusLocationCacheRepository cacheRepository,
            KafkaProducerService kafkaProducerService,
            @Value("${app.simulator.enabled:true}") boolean simulatorEnabled,
            @Value("${app.simulator.speed-kmh:40}") double speedKmh,
            @Value("${app.simulator.interval-ms:5000}") int intervalMs) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.cacheRepository = cacheRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.simulatorEnabled = simulatorEnabled;
        this.speedKmh = speedKmh;
        this.intervalMs = intervalMs;
    }

    /**
     * Run simulation every configured interval
     */
    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:5000}")
    public void simulateGpsUpdates() {
        if (!simulatorEnabled) {
            return;
        }

        log.debug("Running GPS simulation cycle");

        busRepository.findByStatus(Bus.BusStatus.ACTIVE)
                .flatMap(this::simulateBusMovement)
                .doOnError(e -> log.error("Error during GPS simulation", e))
                .subscribe();
    }

    /**
     * Simulate movement for a single bus
     */
    private Mono<Void> simulateBusMovement(Bus bus) {
        // Get or initialize bus state
        SimulatedBusState state = busStates.computeIfAbsent(
                bus.getId(),
                id -> initializeBusState(bus)
        );

        // Calculate new position based on elapsed time and speed
        double distanceKm = (speedKmh * intervalMs) / (1000.0 * 3600.0);
        
        // Update position (simple linear movement for now)
        state.updatePosition(distanceKm);

        // Create location record
        Instant now = Instant.now();
        BusLocation location = BusLocation.builder()
                .busId(bus.getId())
                .vehicleId(bus.getVehicleId())
                .routeId(bus.getCurrentRouteId())
                .latitude(state.getLatitude())
                .longitude(state.getLongitude())
                .speedKmh(speedKmh + randomVariance(5.0))
                .heading(state.getHeading())
                .timestamp(now)
                .source(BusLocation.LocationSource.SIMULATOR)
                .occupancy(ThreadLocalRandom.current().nextInt(0, bus.getCapacity() != null ? bus.getCapacity() : 50))
                .build();

        // Create cache entry
        BusLocationCache cacheEntry = BusLocationCache.builder()
                .busId(bus.getId())
                .vehicleId(bus.getVehicleId())
                .routeId(bus.getCurrentRouteId())
                .latitude(state.getLatitude())
                .longitude(state.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(state.getHeading())
                .timestamp(now)
                .status(bus.getStatus().name())
                .build();

        // Create Kafka event
        BusLocationEvent event = BusLocationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .busId(bus.getId())
                .vehicleId(bus.getVehicleId())
                .routeId(bus.getCurrentRouteId())
                .latitude(state.getLatitude())
                .longitude(state.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(state.getHeading())
                .timestamp(now)
                .source("SIMULATOR")
                .occupancy(location.getOccupancy())
                .build();

        // Save to MongoDB, cache in Redis, and publish to Kafka
        return busLocationRepository.save(location)
                .then(cacheRepository.save(cacheEntry))
                .then(cacheRepository.addToRouteIndex(bus.getId(), bus.getCurrentRouteId()))
                .doOnSuccess(v -> kafkaProducerService.publishLocationEvent(event))
                .doOnSuccess(v -> log.debug("Simulated location for bus {}: {}, {}", 
                        bus.getVehicleId(), state.getLatitude(), state.getLongitude()))
                .then();
    }

    /**
     * Initialize simulated state for a bus
     */
    private SimulatedBusState initializeBusState(Bus bus) {
        // For demo purposes, start buses at predefined locations
        // In production, this would use actual route geometry
        double baseLat = 42.0 + (busStates.size() * 0.01);
        double baseLon = 21.0 + (busStates.size() * 0.01);
        
        return new SimulatedBusState(baseLat, baseLon, 90.0); // Heading east
    }

    /**
     * Add random variance to make simulation more realistic
     */
    private double randomVariance(double maxVariance) {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * maxVariance;
    }

    /**
     * Internal state for simulated bus position
     */
    private static class SimulatedBusState {
        private double latitude;
        private double longitude;
        private double heading;

        public SimulatedBusState(double latitude, double longitude, double heading) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.heading = heading;
        }

        public void updatePosition(double distanceKm) {
            // Simple approximation: 1 degree latitude ≈ 111 km
            // Longitude varies by latitude, but we'll use a simple approximation
            double deltaLat = (distanceKm / 111.0) * Math.cos(Math.toRadians(heading));
            double deltaLon = (distanceKm / 111.0) * Math.sin(Math.toRadians(heading)) 
                    / Math.cos(Math.toRadians(latitude));
            
            this.latitude += deltaLat;
            this.longitude += deltaLon;

            // Occasionally change heading slightly for realistic movement
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                this.heading += ThreadLocalRandom.current().nextDouble(-15, 15);
                this.heading = (this.heading + 360) % 360; // Keep in 0-360 range
            }
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getHeading() {
            return heading;
        }
    }
}
