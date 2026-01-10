package com.cityflow.bus.simulator;

import com.cityflow.bus.dto.RouteStopWithCoordinates;
import com.cityflow.bus.event.BusLocationEvent;
import com.cityflow.bus.model.Bus;
import com.cityflow.bus.model.BusLocation;
import com.cityflow.bus.model.BusLocationCache;
import com.cityflow.bus.repository.BusLocationCacheRepository;
import com.cityflow.bus.repository.BusLocationRepository;
import com.cityflow.bus.repository.BusRepository;
import com.cityflow.bus.service.KafkaProducerService;
import com.cityflow.bus.service.RouteServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
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
    private final RouteServiceClient routeServiceClient;
    private final boolean simulatorEnabled;
    private final String simulatorMode;
    private final double speedKmh;
    private final int intervalMs;
    private final int stopWaitTimeSeconds;

    // Store current position state for each bus
    private final ConcurrentHashMap<String, SimulatedBusState> busStates = new ConcurrentHashMap<>();
    // Cache route stops to avoid repeated API calls
    private final ConcurrentHashMap<UUID, List<RouteStopWithCoordinates>> routeStopsCache = new ConcurrentHashMap<>();

    public GpsSimulator(
            BusRepository busRepository,
            BusLocationRepository busLocationRepository,
            BusLocationCacheRepository cacheRepository,
            KafkaProducerService kafkaProducerService,
            RouteServiceClient routeServiceClient,
            @Value("${app.simulator.enabled:true}") boolean simulatorEnabled,
            @Value("${app.simulator.mode:prishtina}") String simulatorMode,
            @Value("${app.simulator.speed-kmh:40}") double speedKmh,
            @Value("${app.simulator.interval-ms:5000}") int intervalMs,
            @Value("${app.simulator.stop-wait-seconds:30}") int stopWaitTimeSeconds) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.cacheRepository = cacheRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.routeServiceClient = routeServiceClient;
        this.simulatorEnabled = simulatorEnabled;
        this.simulatorMode = simulatorMode;
        this.speedKmh = speedKmh;
        this.intervalMs = intervalMs;
        this.stopWaitTimeSeconds = stopWaitTimeSeconds;
    }

    /**
     * Run simulation every configured interval
     * Only runs in 'generic' mode - for Prishtina routes, use PrishtinaBusSimulator
     */
    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:5000}")
    public void simulateGpsUpdates() {
        if (!simulatorEnabled || !"generic".equalsIgnoreCase(simulatorMode)) {
            return; // Skip if disabled or using Prishtina simulator
        }

        log.debug("Running GPS simulation cycle (generic mode)");

        busRepository.findByStatus(Bus.BusStatus.ACTIVE)
                .flatMap(this::simulateBusMovement)
                .doOnError(e -> log.error("Error during GPS simulation", e))
                .subscribe();
    }

    /**
     * Simulate movement for a single bus
     */
    private Mono<Void> simulateBusMovement(Bus bus) {
        if (bus.getCurrentRouteId() == null) {
            log.debug("Bus {} has no route assigned, skipping simulation", bus.getVehicleId());
            return Mono.empty();
        }

        // Get or initialize bus state
        SimulatedBusState currentState = busStates.computeIfAbsent(
                bus.getId(),
                id -> new SimulatedBusState(bus.getCurrentRouteId()));

        // If route changed, reset state
        final SimulatedBusState state;
        if (!currentState.getRouteId().equals(bus.getCurrentRouteId())) {
            log.info("Route changed for bus {}, resetting state", bus.getVehicleId());
            busStates.put(bus.getId(), new SimulatedBusState(bus.getCurrentRouteId()));
            state = busStates.get(bus.getId());
        } else {
            state = currentState;
        }

        // Fetch route stops if not cached
        return getRouteStops(bus.getCurrentRouteId())
                .flatMap(routeStops -> {
                    if (routeStops.isEmpty()) {
                        log.warn("No stops found for route {}, using fallback simulation", bus.getCurrentRouteId());
                        return simulateFallbackMovement(bus, state);
                    }

                    // Initialize state with first stop if needed
                    if (!state.isInitialized()) {
                        RouteStopWithCoordinates firstStop = routeStops.get(0);
                        state.initialize(firstStop.getLatitude(), firstStop.getLongitude(), routeStops);
                        log.debug("Initialized bus {} at stop {} ({})",
                                bus.getVehicleId(), firstStop.getStopName(), firstStop.getSequenceOrder());
                    }

                    // Update bus position along route
                    return updateBusPositionAlongRoute(bus, state, routeStops);
                })
                .onErrorResume(e -> {
                    log.error("Error simulating movement for bus {}", bus.getVehicleId(), e);
                    return simulateFallbackMovement(bus, state);
                });
    }

    /**
     * Get route stops, using cache if available
     */
    private Mono<List<RouteStopWithCoordinates>> getRouteStops(UUID routeId) {
        List<RouteStopWithCoordinates> cached = routeStopsCache.get(routeId);
        if (cached != null) {
            return Mono.just(cached);
        }

        return routeServiceClient.getRouteStops(routeId)
                .doOnNext(stops -> {
                    if (!stops.isEmpty()) {
                        routeStopsCache.put(routeId, stops);
                    }
                });
    }

    /**
     * Update bus position along the route
     */
    private Mono<Void> updateBusPositionAlongRoute(Bus bus, SimulatedBusState state,
            List<RouteStopWithCoordinates> routeStops) {
        Instant now = Instant.now();

        // Check if bus is waiting at a stop
        if (state.isWaitingAtStop()) {
            long waitTimeElapsed = (now.toEpochMilli() - state.getStopArrivalTime()) / 1000;
            if (waitTimeElapsed < stopWaitTimeSeconds) {
                // Still waiting at stop
                return createLocationUpdate(bus, state, now, 0.0, routeStops)
                        .flatMap(location -> busLocationRepository.save(location)
                                .then(createAndSaveCacheEntry(bus, location))
                                .then(cacheRepository.addToRouteIndex(bus.getId(), bus.getCurrentRouteId()))
                                .doOnSuccess(v -> publishLocationEvent(bus, location))
                                .then());
            } else {
                // Depart from stop
                state.departFromStop();
                log.debug("Bus {} departing from stop {}", bus.getVehicleId(), state.getCurrentStopIndex());
            }
        }

        // Calculate distance to travel this interval
        double distanceKm = (speedKmh * intervalMs) / (1000.0 * 3600.0);

        // Move bus towards next stop
        state.moveTowardsNextStop(distanceKm, routeStops);

        // Check if reached next stop
        if (state.hasReachedNextStop(routeStops)) {
            state.arriveAtStop(now.toEpochMilli());
            RouteStopWithCoordinates currentStop = routeStops.get(state.getCurrentStopIndex());
            log.debug("Bus {} arrived at stop {} ({})",
                    bus.getVehicleId(), currentStop.getStopName(), currentStop.getSequenceOrder());
        }

        // Calculate next stop info
        RouteStopWithCoordinates nextStop = state.getNextStop(routeStops);
        double distanceToNextStop = state.getDistanceToNextStop(routeStops);
        int etaSeconds = nextStop != null ? (int) ((distanceToNextStop / speedKmh) * 3600) : null;

        // Create location update
        return createLocationUpdate(bus, state, now, speedKmh + randomVariance(5.0), routeStops)
                .flatMap(location -> {
                    // Update location with next stop info
                    location.setNextStopId(nextStop != null ? nextStop.getStopId().toString() : null);
                    location.setDistanceToNextStopKm(nextStop != null ? distanceToNextStop : null);
                    location.setEstimatedArrivalSeconds(etaSeconds);

                    // Save and publish
                    return busLocationRepository.save(location)
                            .then(createAndSaveCacheEntry(bus, location))
                            .then(cacheRepository.addToRouteIndex(bus.getId(), bus.getCurrentRouteId()))
                            .doOnSuccess(v -> publishLocationEvent(bus, location))
                            .then();
                });
    }

    /**
     * Fallback simulation when route data is unavailable
     */
    private Mono<Void> simulateFallbackMovement(Bus bus, SimulatedBusState state) {
        if (!state.isInitialized()) {
            double baseLat = 42.0 + (busStates.size() * 0.01);
            double baseLon = 21.0 + (busStates.size() * 0.01);
            state.initializeFallback(baseLat, baseLon, 90.0);
        }

        double distanceKm = (speedKmh * intervalMs) / (1000.0 * 3600.0);
        state.updatePositionFallback(distanceKm);

        Instant now = Instant.now();
        return createLocationUpdate(bus, state, now, speedKmh + randomVariance(5.0), List.of())
                .flatMap(location -> busLocationRepository.save(location)
                        .then(createAndSaveCacheEntry(bus, location))
                        .then(cacheRepository.addToRouteIndex(bus.getId(), bus.getCurrentRouteId()))
                        .doOnSuccess(v -> publishLocationEvent(bus, location))
                        .then());
    }

    /**
     * Create location update
     */
    private Mono<BusLocation> createLocationUpdate(Bus bus, SimulatedBusState state,
            Instant now, double speed,
            List<RouteStopWithCoordinates> routeStops) {
        RouteStopWithCoordinates nextStop = state.getNextStop(routeStops);
        double distanceToNextStop = state.getDistanceToNextStop(routeStops);
        Integer etaSeconds = (nextStop != null && speed > 0) ? (int) ((distanceToNextStop / speed) * 3600) : null;

        BusLocation location = BusLocation.builder()
                .busId(bus.getId())
                .vehicleId(bus.getVehicleId())
                .routeId(bus.getCurrentRouteId())
                .latitude(state.getLatitude())
                .longitude(state.getLongitude())
                .speedKmh(speed)
                .heading(state.getHeading())
                .timestamp(now)
                .source(BusLocation.LocationSource.SIMULATOR)
                .occupancy(ThreadLocalRandom.current().nextInt(0, bus.getCapacity() != null ? bus.getCapacity() : 50))
                .nextStopId(nextStop != null ? nextStop.getStopId().toString() : null)
                .distanceToNextStopKm(nextStop != null ? distanceToNextStop : null)
                .estimatedArrivalSeconds(etaSeconds)
                .build();

        return Mono.just(location);
    }

    /**
     * Create and save cache entry
     */
    private Mono<Void> createAndSaveCacheEntry(Bus bus, BusLocation location) {
        BusLocationCache cacheEntry = BusLocationCache.builder()
                .busId(bus.getId())
                .vehicleId(bus.getVehicleId())
                .routeId(bus.getCurrentRouteId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(location.getHeading())
                .timestamp(location.getTimestamp())
                .status(bus.getStatus().name())
                .nextStopId(location.getNextStopId())
                .distanceToNextStopKm(location.getDistanceToNextStopKm())
                .estimatedArrivalSeconds(location.getEstimatedArrivalSeconds())
                .build();

        return cacheRepository.save(cacheEntry).then();
    }

    /**
     * Publish location event to Kafka
     */
    private void publishLocationEvent(Bus bus, BusLocation location) {
        BusLocationEvent event = BusLocationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .busId(bus.getId())
                .vehicleId(bus.getVehicleId())
                .routeId(bus.getCurrentRouteId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(location.getHeading())
                .timestamp(location.getTimestamp())
                .source("SIMULATOR")
                .occupancy(location.getOccupancy())
                .build();

        kafkaProducerService.publishLocationEvent(event);
    }

    /**
     * Add random variance to make simulation more realistic
     */
    private double randomVariance(double maxVariance) {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * maxVariance;
    }

    /**
     * Calculate distance between two points in kilometers (Haversine formula)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Calculate bearing (heading) from point 1 to point 2 in degrees
     */
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad)
                - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360; // Normalize to 0-360
    }

    /**
     * Internal state for simulated bus position along a route
     */
    private class SimulatedBusState {
        private UUID routeId;
        private double latitude;
        private double longitude;
        private double heading;
        private boolean initialized = false;
        private int currentStopIndex = 0;
        private long stopArrivalTime = 0;
        private boolean waitingAtStop = false;
        private List<RouteStopWithCoordinates> routeStops;

        public SimulatedBusState(UUID routeId) {
            this.routeId = routeId;
        }

        public UUID getRouteId() {
            return routeId;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public void initialize(double lat, double lon, List<RouteStopWithCoordinates> stops) {
            this.latitude = lat;
            this.longitude = lon;
            this.routeStops = stops;
            this.currentStopIndex = 0;
            this.initialized = true;
            updateHeadingToNextStop();
        }

        public void initializeFallback(double lat, double lon, double heading) {
            this.latitude = lat;
            this.longitude = lon;
            this.heading = heading;
            this.initialized = true;
        }

        public void moveTowardsNextStop(double distanceKm, List<RouteStopWithCoordinates> stops) {
            if (stops.isEmpty() || currentStopIndex >= stops.size() - 1) {
                // Reached end of route, loop back to start
                if (!stops.isEmpty()) {
                    currentStopIndex = 0;
                    RouteStopWithCoordinates firstStop = stops.get(0);
                    this.latitude = firstStop.getLatitude();
                    this.longitude = firstStop.getLongitude();
                    updateHeadingToNextStop();
                } else {
                    updatePositionFallback(distanceKm);
                }
                return;
            }

            RouteStopWithCoordinates nextStop = stops.get(currentStopIndex + 1);
            double distanceToNextStop = calculateDistance(latitude, longitude,
                    nextStop.getLatitude(), nextStop.getLongitude());

            if (distanceToNextStop <= distanceKm) {
                // Reached next stop
                this.latitude = nextStop.getLatitude();
                this.longitude = nextStop.getLongitude();
            } else {
                // Move towards next stop
                double bearing = calculateBearing(latitude, longitude,
                        nextStop.getLatitude(), nextStop.getLongitude());
                this.heading = bearing;

                // Move along the bearing
                double deltaLat = (distanceKm / 111.0) * Math.cos(Math.toRadians(bearing));
                double deltaLon = (distanceKm / 111.0) * Math.sin(Math.toRadians(bearing))
                        / Math.cos(Math.toRadians(latitude));

                this.latitude += deltaLat;
                this.longitude += deltaLon;
            }
        }

        public boolean hasReachedNextStop(List<RouteStopWithCoordinates> stops) {
            if (stops.isEmpty() || currentStopIndex >= stops.size() - 1) {
                return false;
            }

            RouteStopWithCoordinates nextStop = stops.get(currentStopIndex + 1);
            double distance = calculateDistance(latitude, longitude,
                    nextStop.getLatitude(), nextStop.getLongitude());

            // Consider reached if within 50 meters
            return distance < 0.05;
        }

        public void arriveAtStop(long arrivalTime) {
            this.waitingAtStop = true;
            this.stopArrivalTime = arrivalTime;
            this.currentStopIndex++;

            if (routeStops != null && currentStopIndex < routeStops.size()) {
                RouteStopWithCoordinates currentStop = routeStops.get(currentStopIndex);
                this.latitude = currentStop.getLatitude();
                this.longitude = currentStop.getLongitude();
            }
        }

        public void departFromStop() {
            this.waitingAtStop = false;
            this.stopArrivalTime = 0;
            updateHeadingToNextStop();
        }

        private void updateHeadingToNextStop() {
            if (routeStops != null && currentStopIndex < routeStops.size() - 1) {
                RouteStopWithCoordinates currentStop = routeStops.get(currentStopIndex);
                RouteStopWithCoordinates nextStop = routeStops.get(currentStopIndex + 1);
                this.heading = calculateBearing(
                        currentStop.getLatitude(), currentStop.getLongitude(),
                        nextStop.getLatitude(), nextStop.getLongitude());
            }
        }

        public RouteStopWithCoordinates getNextStop(List<RouteStopWithCoordinates> stops) {
            if (stops.isEmpty() || currentStopIndex >= stops.size() - 1) {
                return null;
            }
            return stops.get(currentStopIndex + 1);
        }

        public double getDistanceToNextStop(List<RouteStopWithCoordinates> stops) {
            RouteStopWithCoordinates nextStop = getNextStop(stops);
            if (nextStop == null) {
                return 0.0;
            }
            return calculateDistance(latitude, longitude,
                    nextStop.getLatitude(), nextStop.getLongitude());
        }

        public void updatePositionFallback(double distanceKm) {
            double deltaLat = (distanceKm / 111.0) * Math.cos(Math.toRadians(heading));
            double deltaLon = (distanceKm / 111.0) * Math.sin(Math.toRadians(heading))
                    / Math.cos(Math.toRadians(latitude));

            this.latitude += deltaLat;
            this.longitude += deltaLon;

            // Occasionally change heading slightly for realistic movement
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                this.heading += ThreadLocalRandom.current().nextDouble(-15, 15);
                this.heading = (this.heading + 360) % 360;
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

        public int getCurrentStopIndex() {
            return currentStopIndex;
        }

        public boolean isWaitingAtStop() {
            return waitingAtStop;
        }

        public long getStopArrivalTime() {
            return stopArrivalTime;
        }
    }
}
