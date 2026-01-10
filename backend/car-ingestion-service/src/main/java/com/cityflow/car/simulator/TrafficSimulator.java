package com.cityflow.car.simulator;

import com.cityflow.car.event.CarLocationEvent;
import com.cityflow.car.model.Car;
import com.cityflow.car.model.CarLocation;
import com.cityflow.car.model.CarLocationCache;
import com.cityflow.car.repository.CarLocationCacheRepository;
import com.cityflow.car.repository.CarLocationRepository;
import com.cityflow.car.repository.CarRepository;
import com.cityflow.car.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Realistic traffic simulator for Prishtina.
 * Cars follow actual roads, cluster at congestion hotspots, and react to traffic density.
 */
@Component
public class TrafficSimulator {

    private static final Logger log = LoggerFactory.getLogger(TrafficSimulator.class);

    private final CarRepository carRepository;
    private final CarLocationRepository carLocationRepository;
    private final CarLocationCacheRepository cacheRepository;
    private final KafkaProducerService kafkaProducerService;
    private final boolean simulatorEnabled;
    private final double baseSpeedKmh;
    private final double maxSpeedKmh;
    private final int intervalMs;
    private final int initialCarCount;
    private final double trafficDensityRadiusKm;

    // Store current position state for each car
    private final ConcurrentHashMap<String, RoadFollowingCarState> carStates = new ConcurrentHashMap<>();

    public TrafficSimulator(
            CarRepository carRepository,
            CarLocationRepository carLocationRepository,
            CarLocationCacheRepository cacheRepository,
            KafkaProducerService kafkaProducerService,
            @Value("${app.simulator.enabled:true}") boolean simulatorEnabled,
            @Value("${app.simulator.base-speed-kmh:50}") double baseSpeedKmh,
            @Value("${app.simulator.max-speed-kmh:80}") double maxSpeedKmh,
            @Value("${app.simulator.interval-ms:3000}") int intervalMs,
            @Value("${app.simulator.initial-car-count:100}") int initialCarCount,
            @Value("${app.simulator.traffic-density-radius-km:0.5}") double trafficDensityRadiusKm) {
        this.carRepository = carRepository;
        this.carLocationRepository = carLocationRepository;
        this.cacheRepository = cacheRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.simulatorEnabled = simulatorEnabled;
        this.baseSpeedKmh = baseSpeedKmh;
        this.maxSpeedKmh = maxSpeedKmh;
        this.intervalMs = intervalMs;
        this.initialCarCount = initialCarCount;
        this.trafficDensityRadiusKm = trafficDensityRadiusKm;
    }

    /**
     * Run simulation every configured interval
     */
    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:3000}")
    public void simulateTrafficFlow() {
        if (!simulatorEnabled) {
            return;
        }

        log.debug("Running traffic simulation cycle");

        // Get all active cars
        carRepository.findByStatus(Car.CarStatus.ACTIVE)
                .collectList()
                .flatMapMany(cars -> {
                    // Ensure we have enough cars
                    if (cars.size() < initialCarCount) {
                        int carsToCreate = initialCarCount - cars.size();
                        log.info("Creating {} additional cars for simulation", carsToCreate);
                        return createInitialCars(carsToCreate)
                                .thenMany(carRepository.findByStatus(Car.CarStatus.ACTIVE));
                    }
                    return Flux.fromIterable(cars);
                })
                .flatMap(this::simulateCarMovement)
                .doOnError(e -> log.error("Error during traffic simulation", e))
                .subscribe();
    }

    /**
     * Create initial cars for simulation
     */
    private Mono<Void> createInitialCars(int count) {
        return Flux.range(0, count)
                .flatMap(i -> {
                    Car car = Car.builder()
                            .vehicleId("CAR-" + String.format("%05d", ThreadLocalRandom.current().nextInt(100000)))
                            .licensePlate(generateLicensePlate())
                            .status(Car.CarStatus.ACTIVE)
                            .make(generateRandomMake())
                            .model(generateRandomModel())
                            .color(generateRandomColor())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return carRepository.save(car);
                })
                .then();
    }

    /**
     * Simulate movement for a single car following actual roads
     */
    private Mono<Void> simulateCarMovement(Car car) {
        // Get or initialize car state on a random road
        RoadFollowingCarState state = carStates.computeIfAbsent(
                car.getId(),
                id -> new RoadFollowingCarState(PrishtinaRoadNetwork.getRandomRoad()));

        // Get current hour for congestion calculation
        int currentHour = Instant.now().atZone(ZoneId.systemDefault()).getHour();

        // Calculate traffic density around this car
        return calculateTrafficDensity(state.getLatitude(), state.getLongitude())
                .flatMap(trafficMetrics -> {
                    // Update car position based on road and traffic conditions
                    updateRoadFollowingPosition(state, trafficMetrics, currentHour);

                    // Create location update
                    Instant now = Instant.now();
                    return createLocationUpdate(car, state, now, trafficMetrics)
                            .flatMap(location -> {
                                // Save and publish
                                return carLocationRepository.save(location)
                                        .then(createAndSaveCacheEntry(car, location))
                                        .doOnSuccess(v -> publishLocationEvent(car, location))
                                        .then();
                            });
                });
    }

    /**
     * Calculate traffic density and congestion around a location
     */
    private Mono<TrafficMetrics> calculateTrafficDensity(double lat, double lon) {
        // Get all nearby cars from cache (within radius)
        return cacheRepository.findAll()
                .collectList()
                .map(caches -> {
                    int nearbyCount = 0;
                    double totalSpeed = 0.0;
                    int speedCount = 0;

                    for (CarLocationCache cache : caches) {
                        double distance = calculateDistance(lat, lon, 
                                cache.getLatitude(), cache.getLongitude());
                        if (distance <= trafficDensityRadiusKm) {
                            nearbyCount++;
                            if (cache.getSpeedKmh() != null) {
                                totalSpeed += cache.getSpeedKmh();
                                speedCount++;
                            }
                        }
                    }

                    // Calculate density (cars per km²)
                    double areaKm2 = Math.PI * trafficDensityRadiusKm * trafficDensityRadiusKm;
                    double density = nearbyCount / areaKm2;

                    // Calculate congestion level (0.0 = free, 1.0 = gridlock)
                    // Based on density and average speed
                    double avgSpeed = speedCount > 0 ? totalSpeed / speedCount : baseSpeedKmh;
                    double congestionLevel = Math.min(1.0, 
                            Math.max(0.0, 1.0 - (avgSpeed / maxSpeedKmh)));

                    return new TrafficMetrics(density, congestionLevel, nearbyCount);
                })
                .defaultIfEmpty(new TrafficMetrics(0.0, 0.0, 0));
    }

    /**
     * Update car position following roads with congestion awareness
     */
    private void updateRoadFollowingPosition(RoadFollowingCarState state, TrafficMetrics trafficMetrics, int currentHour) {
        PrishtinaRoadNetwork.Road road = state.getCurrentRoad();
        
        // Get road speed limit
        double roadSpeedLimit = road.speedLimitKmh();
        
        // Calculate congestion at current location from hotspots
        double congestionSlowdown = 1.0;
        for (PrishtinaRoadNetwork.CongestionHotspot hotspot : PrishtinaRoadNetwork.getCongestionHotspots()) {
            if (hotspot.isInHotspot(state.getLatitude(), state.getLongitude())) {
                double hotspotSlowdown = 1.0 - hotspot.getCongestionMultiplier(currentHour);
                congestionSlowdown = Math.min(congestionSlowdown, hotspotSlowdown);
            }
        }
        
        // Adjust speed based on traffic density and congestion
        double speedMultiplier = calculateSpeedMultiplier(trafficMetrics);
        speedMultiplier *= congestionSlowdown;
        
        double adjustedSpeedKmh = roadSpeedLimit * speedMultiplier;
        adjustedSpeedKmh = Math.max(adjustedSpeedKmh, 3.0); // Minimum 3 km/h even in gridlock
        adjustedSpeedKmh = Math.min(adjustedSpeedKmh, maxSpeedKmh);

        state.setCurrentSpeed(adjustedSpeedKmh);

        // Calculate distance to travel this interval
        double distanceKm = (adjustedSpeedKmh * intervalMs) / (1000.0 * 3600.0);

        // Move towards next waypoint on the road
        state.moveAlongRoad(distanceKm);

        // Check if car has reached end of road
        if (state.hasReachedEndOfRoad()) {
            // Switch to a connected road or start the same road in reverse
            List<PrishtinaRoadNetwork.Road> nearbyRoads = PrishtinaRoadNetwork.getRoadsNearLocation(
                    state.getLatitude(), state.getLongitude(), 0.1);
            
            if (!nearbyRoads.isEmpty() && ThreadLocalRandom.current().nextDouble() < 0.7) {
                // 70% chance to switch to a different nearby road
                PrishtinaRoadNetwork.Road newRoad = nearbyRoads.get(
                        ThreadLocalRandom.current().nextInt(nearbyRoads.size()));
                state.switchToRoad(newRoad);
            } else {
                // Reverse direction on current road
                state.reverseDirection();
            }
        }
    }

    /**
     * Calculate speed multiplier based on traffic conditions
     * Returns a value between 0.1 (heavy congestion) and 1.0 (free flow)
     */
    private double calculateSpeedMultiplier(TrafficMetrics trafficMetrics) {
        double densityFactor = Math.min(1.0, trafficMetrics.density / 100.0); // Normalize density
        double congestionFactor = trafficMetrics.congestionLevel;

        // Combine factors: higher density and congestion = slower speed
        double speedReduction = (densityFactor * 0.5) + (congestionFactor * 0.5);
        return Math.max(0.1, 1.0 - speedReduction);
    }

    /**
     * Create location update
     */
    private Mono<CarLocation> createLocationUpdate(Car car, RoadFollowingCarState state,
            Instant now, TrafficMetrics trafficMetrics) {
        CarLocation location = CarLocation.builder()
                .carId(car.getId())
                .vehicleId(car.getVehicleId())
                .latitude(state.getLatitude())
                .longitude(state.getLongitude())
                .speedKmh(state.getCurrentSpeed())
                .heading(state.getHeading())
                .timestamp(now)
                .source(CarLocation.LocationSource.SIMULATOR)
                .trafficDensity(trafficMetrics.density)
                .congestionLevel(trafficMetrics.congestionLevel)
                .roadId(state.getCurrentRoad().id())
                .roadName(state.getCurrentRoad().name())
                .build();

        return Mono.just(location);
    }

    /**
     * Create and save cache entry
     */
    private Mono<Void> createAndSaveCacheEntry(Car car, CarLocation location) {
        CarLocationCache cacheEntry = CarLocationCache.builder()
                .carId(car.getId())
                .vehicleId(car.getVehicleId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(location.getHeading())
                .timestamp(location.getTimestamp())
                .status(car.getStatus().name())
                .trafficDensity(location.getTrafficDensity())
                .congestionLevel(location.getCongestionLevel())
                .build();

        return cacheRepository.save(cacheEntry).then();
    }

    /**
     * Publish location event to Kafka
     */
    private void publishLocationEvent(Car car, CarLocation location) {
        CarLocationEvent event = CarLocationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .carId(car.getId())
                .vehicleId(car.getVehicleId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .speedKmh(location.getSpeedKmh())
                .heading(location.getHeading())
                .timestamp(location.getTimestamp())
                .source("SIMULATOR")
                .trafficDensity(location.getTrafficDensity())
                .congestionLevel(location.getCongestionLevel())
                .build();

        kafkaProducerService.publishLocationEvent(event);
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

    private String generateLicensePlate() {
        // Kosovo license plate format: XX-NNN-XX
        String region = ThreadLocalRandom.current().nextBoolean() ? "PR" : 
                (ThreadLocalRandom.current().nextBoolean() ? "KS" : "PE");
        return String.format("%s-%03d-%c%c", 
                region,
                ThreadLocalRandom.current().nextInt(100, 999),
                (char) ('A' + ThreadLocalRandom.current().nextInt(26)),
                (char) ('A' + ThreadLocalRandom.current().nextInt(26)));
    }

    private String generateRandomMake() {
        // Popular car makes in Kosovo
        String[] makes = {"Volkswagen", "Mercedes", "BMW", "Audi", "Opel", "Ford", "Renault", "Peugeot", "Toyota", "Hyundai", "Skoda", "Fiat"};
        return makes[ThreadLocalRandom.current().nextInt(makes.length)];
    }

    private String generateRandomModel() {
        String[] models = {"Sedan", "SUV", "Hatchback", "Coupe", "Wagon", "Van"};
        return models[ThreadLocalRandom.current().nextInt(models.length)];
    }

    private String generateRandomColor() {
        String[] colors = {"Black", "White", "Silver", "Gray", "Blue", "Red", "Green", "Beige"};
        return colors[ThreadLocalRandom.current().nextInt(colors.length)];
    }

    /**
     * Car state that follows roads through waypoints
     */
    private class RoadFollowingCarState {
        private PrishtinaRoadNetwork.Road currentRoad;
        private int currentWaypointIndex;
        private int direction; // 1 = forward, -1 = backward
        private double latitude;
        private double longitude;
        private double heading;
        private double currentSpeed;
        private double progressToNextWaypoint; // 0.0 to 1.0

        public RoadFollowingCarState(PrishtinaRoadNetwork.Road road) {
            this.currentRoad = road;
            this.direction = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            
            // Start at a random position along the road
            List<double[]> waypoints = road.waypoints();
            this.currentWaypointIndex = ThreadLocalRandom.current().nextInt(waypoints.size() - 1);
            this.progressToNextWaypoint = ThreadLocalRandom.current().nextDouble();
            
            // Calculate initial position
            updatePositionFromWaypoints();
            this.currentSpeed = road.speedLimitKmh() * (0.7 + ThreadLocalRandom.current().nextDouble() * 0.3);
        }

        private void updatePositionFromWaypoints() {
            List<double[]> waypoints = currentRoad.waypoints();
            int nextIndex = getNextWaypointIndex();
            
            double[] current = waypoints.get(currentWaypointIndex);
            double[] next = waypoints.get(nextIndex);
            
            // Interpolate position between waypoints
            this.latitude = current[0] + (next[0] - current[0]) * progressToNextWaypoint;
            this.longitude = current[1] + (next[1] - current[1]) * progressToNextWaypoint;
            
            // Calculate heading
            this.heading = calculateBearing(current[0], current[1], next[0], next[1]);
        }

        public void moveAlongRoad(double distanceKm) {
            List<double[]> waypoints = currentRoad.waypoints();
            int nextIndex = getNextWaypointIndex();
            
            double[] current = waypoints.get(currentWaypointIndex);
            double[] next = waypoints.get(nextIndex);
            
            // Calculate distance between current and next waypoint
            double waypointDistance = calculateDistance(current[0], current[1], next[0], next[1]);
            
            if (waypointDistance > 0) {
                // Calculate how much progress this distance represents
                double progressIncrement = distanceKm / waypointDistance;
                progressToNextWaypoint += progressIncrement;
                
                // Add slight lane variation for realism
                double laneOffset = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.0001;
                
                // Check if we've passed the next waypoint
                while (progressToNextWaypoint >= 1.0) {
                    progressToNextWaypoint -= 1.0;
                    currentWaypointIndex = nextIndex;
                    nextIndex = getNextWaypointIndex();
                    
                    if (hasReachedEndOfRoad()) {
                        progressToNextWaypoint = 0.0;
                        break;
                    }
                }
                
                // Update position with lane offset
                updatePositionFromWaypoints();
                this.latitude += laneOffset;
            }
        }

        private int getNextWaypointIndex() {
            int next = currentWaypointIndex + direction;
            return Math.max(0, Math.min(next, currentRoad.waypoints().size() - 1));
        }

        public boolean hasReachedEndOfRoad() {
            if (direction > 0) {
                return currentWaypointIndex >= currentRoad.waypoints().size() - 1;
            } else {
                return currentWaypointIndex <= 0;
            }
        }

        public void reverseDirection() {
            direction *= -1;
            progressToNextWaypoint = 0.0;
        }

        public void switchToRoad(PrishtinaRoadNetwork.Road newRoad) {
            // Find closest waypoint on new road
            double minDistance = Double.MAX_VALUE;
            int closestIndex = 0;
            
            for (int i = 0; i < newRoad.waypoints().size(); i++) {
                double[] wp = newRoad.waypoints().get(i);
                double dist = calculateDistance(latitude, longitude, wp[0], wp[1]);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestIndex = i;
                }
            }
            
            this.currentRoad = newRoad;
            this.currentWaypointIndex = closestIndex;
            this.progressToNextWaypoint = 0.0;
            this.direction = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        }

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

        // Getters
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getHeading() { return heading; }
        public double getCurrentSpeed() { return currentSpeed; }
        public void setCurrentSpeed(double speed) { this.currentSpeed = speed; }
        public PrishtinaRoadNetwork.Road getCurrentRoad() { return currentRoad; }
    }

    /**
     * Traffic metrics for a location
     */
    private static class TrafficMetrics {
        final double density; // Cars per km²
        final double congestionLevel; // 0.0 to 1.0
        final int nearbyCount; // Number of cars within radius

        TrafficMetrics(double density, double congestionLevel, int nearbyCount) {
            this.density = density;
            this.congestionLevel = congestionLevel;
            this.nearbyCount = nearbyCount;
        }
    }
}

