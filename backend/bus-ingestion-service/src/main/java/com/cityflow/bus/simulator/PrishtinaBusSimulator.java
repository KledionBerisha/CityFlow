package com.cityflow.bus.simulator;

import com.cityflow.bus.event.BusLocationEvent;
import com.cityflow.bus.model.Bus;
import com.cityflow.bus.model.BusLocation;
import com.cityflow.bus.model.BusLocationCache;
import com.cityflow.bus.repository.BusLocationCacheRepository;
import com.cityflow.bus.repository.BusLocationRepository;
import com.cityflow.bus.repository.BusRepository;
import com.cityflow.bus.service.KafkaProducerService;
import com.cityflow.bus.simulator.PrishtinaBusRoutes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PrishtinaBusSimulator {

    private static final Logger log = LoggerFactory.getLogger(PrishtinaBusSimulator.class);
    private static final ZoneId PRISHTINA_ZONE = ZoneId.of("Europe/Belgrade");

    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final BusLocationCacheRepository cacheRepository;
    private final KafkaProducerService kafkaProducerService;
    private final boolean simulatorEnabled;
    private final String simulatorMode;
    private final int intervalMs;
    private final int stopWaitTimeSeconds;

    // Store state for each simulated bus
    private final ConcurrentHashMap<String, PrishtinaBusState> busStates = new ConcurrentHashMap<>();
    
    // Track if buses have been initialized
    private volatile boolean initialized = false;

    public PrishtinaBusSimulator(
            BusRepository busRepository,
            BusLocationRepository busLocationRepository,
            BusLocationCacheRepository cacheRepository,
            KafkaProducerService kafkaProducerService,
            @Value("${app.simulator.enabled:true}") boolean simulatorEnabled,
            @Value("${app.simulator.mode:prishtina}") String simulatorMode,
            @Value("${app.simulator.interval-ms:5000}") int intervalMs,
            @Value("${app.simulator.stop-wait-seconds:20}") int stopWaitTimeSeconds) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.cacheRepository = cacheRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.simulatorEnabled = simulatorEnabled;
        this.simulatorMode = simulatorMode;
        this.intervalMs = intervalMs;
        this.stopWaitTimeSeconds = stopWaitTimeSeconds;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBuses() {
        if (!simulatorEnabled || !"prishtina".equalsIgnoreCase(simulatorMode)) {
            log.info("Prishtina bus simulator is disabled (mode={})", simulatorMode);
            return;
        }

        log.info("Initializing Prishtina bus simulator with {} real Trafiku Urban routes", 
            PrishtinaBusRoutes.ALL_ROUTES.size());

        // Create or update buses for each vehicle
        Flux.fromIterable(PrishtinaBusRoutes.VEHICLES.values())
            .flatMap(vehicle -> {
                BusRoute foundRoute = PrishtinaBusRoutes.getRouteByLine(vehicle.assignedRoute().replace("LINE-", ""));
                final BusRoute route = (foundRoute != null) ? foundRoute : PrishtinaBusRoutes.LINE_1;

                return busRepository.findByVehicleId(vehicle.vehicleId())
                    .switchIfEmpty(Mono.defer(() -> {
                        Bus bus = Bus.builder()
                            .vehicleId(vehicle.vehicleId())
                            .licensePlate(vehicle.licensePlate())
                            .model(vehicle.model())
                            .capacity(vehicle.passengerCapacity())
                            .status(Bus.BusStatus.ACTIVE)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                        log.info("Creating new bus: {} for {}", vehicle.vehicleId(), route.name());
                        return busRepository.save(bus);
                    }))
                    .flatMap(bus -> {
                        // Initialize bus state
                        initializeBusState(bus, route);
                        return Mono.just(bus);
                    });
            })
            .collectList()
            .subscribe(
                buses -> {
                    initialized = true;
                    log.info("✅ Prishtina bus simulator initialized with {} buses", buses.size());
                    buses.forEach(bus -> log.info("  - {} on route {}", bus.getVehicleId(), 
                        busStates.get(bus.getId()) != null ? busStates.get(bus.getId()).route.name() : "unknown"));
                },
                error -> log.error("Failed to initialize Prishtina buses", error)
            );
    }

    /**
     * Initialize state for a bus
     */
    private void initializeBusState(Bus bus, BusRoute route) {
        // Start at a random position along the route
        int startStopIndex = ThreadLocalRandom.current().nextInt(route.stops().size());
        BusStop startStop = route.stops().get(startStopIndex);
        
        // Randomly choose direction (outbound or return)
        boolean outbound = ThreadLocalRandom.current().nextBoolean();

        PrishtinaBusState state = new PrishtinaBusState(
            route,
            startStopIndex,
            outbound,
            startStop.latitude(),
            startStop.longitude(),
            route.stops().size() > startStopIndex + 1 ? 
                calculateBearing(startStop.latitude(), startStop.longitude(), 
                    route.stops().get(startStopIndex + 1).latitude(), 
                    route.stops().get(startStopIndex + 1).longitude()) : 0
        );

        busStates.put(bus.getId(), state);
        log.debug("Initialized bus {} at stop {} ({}), direction: {}", 
            bus.getVehicleId(), startStop.name(), startStopIndex, outbound ? "outbound" : "return");
    }

    /**
     * Run simulation every configured interval
     */
    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:5000}")
    public void simulatePrishtinaBuses() {
        if (!simulatorEnabled || !initialized) {
            return;
        }

        LocalTime now = LocalTime.now(PRISHTINA_ZONE);
        log.debug("Running Prishtina bus simulation at {}", now);

        busRepository.findByStatus(Bus.BusStatus.ACTIVE)
            .flatMap(bus -> simulateBusMovement(bus, now))
            .doOnError(e -> log.error("Error during Prishtina bus simulation", e))
            .subscribe();
    }

    /**
     * Simulate movement for a single bus
     */
    private Mono<Void> simulateBusMovement(Bus bus, LocalTime currentTime) {
        PrishtinaBusState state = busStates.get(bus.getId());
        if (state == null) {
            log.debug("No state found for bus {}, skipping", bus.getVehicleId());
            return Mono.empty();
        }

        BusRoute route = state.route;

        // Check if bus should be operating at this time
        if (!route.isOperating(currentTime)) {
            log.debug("Bus {} on {} is not operating at {}", 
                bus.getVehicleId(), route.name(), currentTime);
            return Mono.empty();
        }

        Instant now = Instant.now();

        // Check if bus is waiting at a stop
        if (state.isWaitingAtStop()) {
            long waitTimeElapsed = (now.toEpochMilli() - state.stopArrivalTime) / 1000;
            if (waitTimeElapsed < stopWaitTimeSeconds) {
                // Still waiting - publish stationary location
                return publishBusLocation(bus, state, now, 0.0);
            } else {
                // Depart from stop
                state.departFromStop();
                log.debug("Bus {} departing from {}", bus.getVehicleId(), 
                    route.stops().get(state.currentStopIndex).name());
            }
        }

        // Calculate speed based on route type
        double baseSpeed = route.lineNumber().equals("1A") ? 60.0 : 35.0; // Airport express faster
        
        // Add time-of-day variation (slower during peak hours due to traffic)
        int hour = currentTime.getHour();
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19);
        if (isPeakHour && !route.lineNumber().equals("1A")) {
            baseSpeed *= 0.6; // 40% slower during peak hours in city
        }

        double speed = baseSpeed + randomVariance(5.0);
        double distanceKm = (speed * intervalMs) / (1000.0 * 3600.0);

        // Move bus towards next stop
        moveBusAlongRoute(state, distanceKm);

        // Check if reached next stop
        if (hasReachedNextStop(state)) {
            arriveAtStop(state, now.toEpochMilli());
            BusStop currentStop = route.stops().get(state.currentStopIndex);
            log.debug("Bus {} arrived at {} (stop {})", 
                bus.getVehicleId(), currentStop.name(), state.currentStopIndex);
        }

        return publishBusLocation(bus, state, now, speed);
    }

    /**
     * Move bus along its route
     */
    private void moveBusAlongRoute(PrishtinaBusState state, double distanceKm) {
        BusRoute route = state.route;
        List<BusStop> stops = route.stops();

        if (stops.isEmpty()) return;

        // Get next stop index based on direction
        int nextStopIndex = state.outbound ? state.currentStopIndex + 1 : state.currentStopIndex - 1;

        // Check if at end of route - reverse direction
        if (nextStopIndex >= stops.size() || nextStopIndex < 0) {
            state.outbound = !state.outbound;
            nextStopIndex = state.outbound ? state.currentStopIndex + 1 : state.currentStopIndex - 1;
            
            // If still invalid, stay at current stop
            if (nextStopIndex >= stops.size() || nextStopIndex < 0) {
                return;
            }
            log.debug("Bus on {} reversing direction at {}", 
                route.name(), stops.get(state.currentStopIndex).name());
        }

        BusStop nextStop = stops.get(nextStopIndex);
        double distanceToNextStop = calculateDistance(
            state.latitude, state.longitude, nextStop.latitude(), nextStop.longitude());

        if (distanceToNextStop <= distanceKm) {
            // Reached next stop
            state.latitude = nextStop.latitude();
            state.longitude = nextStop.longitude();
        } else {
            // Move towards next stop
            double bearing = calculateBearing(
                state.latitude, state.longitude, nextStop.latitude(), nextStop.longitude());
            state.heading = bearing;

            // Calculate new position
            double deltaLat = (distanceKm / 111.0) * Math.cos(Math.toRadians(bearing));
            double deltaLon = (distanceKm / 111.0) * Math.sin(Math.toRadians(bearing))
                    / Math.cos(Math.toRadians(state.latitude));

            state.latitude += deltaLat;
            state.longitude += deltaLon;
        }
    }

    /**
     * Check if bus has reached the next stop
     */
    private boolean hasReachedNextStop(PrishtinaBusState state) {
        BusRoute route = state.route;
        List<BusStop> stops = route.stops();

        int nextStopIndex = state.outbound ? state.currentStopIndex + 1 : state.currentStopIndex - 1;
        if (nextStopIndex >= stops.size() || nextStopIndex < 0) {
            return false;
        }

        BusStop nextStop = stops.get(nextStopIndex);
        double distance = calculateDistance(
            state.latitude, state.longitude, nextStop.latitude(), nextStop.longitude());

        return distance < 0.03; // Within 30 meters
    }

    /**
     * Handle arrival at a stop
     */
    private void arriveAtStop(PrishtinaBusState state, long arrivalTime) {
        state.waitingAtStop = true;
        state.stopArrivalTime = arrivalTime;
        state.currentStopIndex = state.outbound ? 
            state.currentStopIndex + 1 : state.currentStopIndex - 1;

        BusStop currentStop = state.route.stops().get(state.currentStopIndex);
        state.latitude = currentStop.latitude();
        state.longitude = currentStop.longitude();
    }

    /**
     * Publish bus location update
     */
    private Mono<Void> publishBusLocation(Bus bus, PrishtinaBusState state, Instant now, double speed) {
        BusRoute route = state.route;
        List<BusStop> stops = route.stops();

        // Calculate next stop info
        int nextStopIndex = state.outbound ? state.currentStopIndex + 1 : state.currentStopIndex - 1;
        BusStop nextStop = (nextStopIndex >= 0 && nextStopIndex < stops.size()) ? 
            stops.get(nextStopIndex) : null;
        
        double distanceToNextStop = nextStop != null ? 
            calculateDistance(state.latitude, state.longitude, nextStop.latitude(), nextStop.longitude()) : 0;
        
        Integer etaSeconds = (nextStop != null && speed > 0) ? 
            (int) ((distanceToNextStop / speed) * 3600) : null;

        // Current stop name for display
        BusStop currentStop = stops.get(state.currentStopIndex);

        BusLocation location = BusLocation.builder()
            .busId(bus.getId())
            .vehicleId(bus.getVehicleId())
            .routeId(null) // Using lineNumber instead
            .latitude(state.latitude)
            .longitude(state.longitude)
            .speedKmh(speed)
            .heading(state.heading)
            .timestamp(now)
            .source(BusLocation.LocationSource.SIMULATOR)
            .occupancy(ThreadLocalRandom.current().nextInt(10, 45))
            .nextStopId(nextStop != null ? nextStop.name() : null)
            .distanceToNextStopKm(nextStop != null ? distanceToNextStop : null)
            .estimatedArrivalSeconds(etaSeconds)
            .lineNumber(route.lineNumber())
            .routeName(route.name())
            .build();

        // Create cache entry with route info
        BusLocationCache cacheEntry = BusLocationCache.builder()
            .busId(bus.getId())
            .vehicleId(bus.getVehicleId())
            .routeId(null)
            .latitude(state.latitude)
            .longitude(state.longitude)
            .speedKmh(speed)
            .heading(state.heading)
            .timestamp(now)
            .status(bus.getStatus().name())
            .nextStopId(nextStop != null ? nextStop.name() : null)
            .distanceToNextStopKm(nextStop != null ? distanceToNextStop : null)
            .estimatedArrivalSeconds(etaSeconds)
            .lineNumber(route.lineNumber())
            .routeName(route.name())
            .build();

        // Publish event
        BusLocationEvent event = BusLocationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .busId(bus.getId())
            .vehicleId(bus.getVehicleId())
            .routeId(null)
            .latitude(state.latitude)
            .longitude(state.longitude)
            .speedKmh(speed)
            .heading(state.heading)
            .timestamp(now)
            .source("TRAFIKU_URBAN_SIM")
            .occupancy(location.getOccupancy())
            .build();

        log.debug("Bus {} [{}] at ({}, {}) speed={} km/h, next={}", 
            bus.getVehicleId(), route.lineNumber(), 
            String.format("%.4f", state.latitude), String.format("%.4f", state.longitude),
            String.format("%.1f", speed), nextStop != null ? nextStop.name() : "END");

        return busLocationRepository.save(location)
            .then(cacheRepository.save(cacheEntry))
            .doOnSuccess(v -> kafkaProducerService.publishLocationEvent(event))
            .then();
    }

    /**
     * Calculate distance between two points in kilometers (Haversine formula)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calculate bearing between two points
     */
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad)
                - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    /**
     * Add random variance
     */
    private double randomVariance(double maxVariance) {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * maxVariance;
    }

    /**
     * Internal state for a Prishtina bus
     */
    private static class PrishtinaBusState {
        BusRoute route;
        int currentStopIndex;
        boolean outbound; // true = going away from terminal, false = returning
        double latitude;
        double longitude;
        double heading;
        boolean waitingAtStop = false;
        long stopArrivalTime = 0;

        PrishtinaBusState(BusRoute route, int currentStopIndex, boolean outbound, 
                         double lat, double lon, double heading) {
            this.route = route;
            this.currentStopIndex = currentStopIndex;
            this.outbound = outbound;
            this.latitude = lat;
            this.longitude = lon;
            this.heading = heading;
        }

        boolean isWaitingAtStop() {
            return waitingAtStop;
        }

        void departFromStop() {
            waitingAtStop = false;
            stopArrivalTime = 0;
        }
    }
}
