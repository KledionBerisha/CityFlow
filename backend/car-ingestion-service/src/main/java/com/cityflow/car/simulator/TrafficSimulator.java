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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    private final double cityBoundsMinLat;
    private final double cityBoundsMaxLat;
    private final double cityBoundsMinLon;
    private final double cityBoundsMaxLon;
    private final double trafficDensityRadiusKm; // Radius for calculating nearby traffic

    // Store current position state for each car
    private final ConcurrentHashMap<String, SimulatedCarState> carStates = new ConcurrentHashMap<>();

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
            @Value("${app.simulator.city-bounds.min-lat:42.0}") double cityBoundsMinLat,
            @Value("${app.simulator.city-bounds.max-lat:42.5}") double cityBoundsMaxLat,
            @Value("${app.simulator.city-bounds.min-lon:21.0}") double cityBoundsMinLon,
            @Value("${app.simulator.city-bounds.max-lon:21.5}") double cityBoundsMaxLon,
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
        this.cityBoundsMinLat = cityBoundsMinLat;
        this.cityBoundsMaxLat = cityBoundsMaxLat;
        this.cityBoundsMinLon = cityBoundsMinLon;
        this.cityBoundsMaxLon = cityBoundsMaxLon;
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
     * Simulate movement for a single car
     */
    private Mono<Void> simulateCarMovement(Car car) {
        // Get or initialize car state
        SimulatedCarState state = carStates.computeIfAbsent(
                car.getId(),
                id -> new SimulatedCarState(
                        randomLatitude(),
                        randomLongitude(),
                        ThreadLocalRandom.current().nextDouble(0, 360)));

        // Calculate traffic density around this car
        return calculateTrafficDensity(state.getLatitude(), state.getLongitude())
                .flatMap(trafficMetrics -> {
                    // Update car position based on traffic conditions
                    updateCarPosition(state, trafficMetrics);

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
     * Update car position based on traffic conditions
     */
    private void updateCarPosition(SimulatedCarState state, TrafficMetrics trafficMetrics) {
        // Calculate distance to travel this interval
        double baseDistanceKm = (baseSpeedKmh * intervalMs) / (1000.0 * 3600.0);

        // Adjust speed based on traffic density and congestion
        double speedMultiplier = calculateSpeedMultiplier(trafficMetrics);
        double adjustedSpeedKmh = baseSpeedKmh * speedMultiplier;
        adjustedSpeedKmh = Math.min(adjustedSpeedKmh, maxSpeedKmh);
        adjustedSpeedKmh = Math.max(adjustedSpeedKmh, 5.0); // Minimum 5 km/h

        double distanceKm = (adjustedSpeedKmh * intervalMs) / (1000.0 * 3600.0);
        state.setCurrentSpeed(adjustedSpeedKmh);

        // Move car in current direction
        double deltaLat = (distanceKm / 111.0) * Math.cos(Math.toRadians(state.getHeading()));
        double deltaLon = (distanceKm / 111.0) * Math.sin(Math.toRadians(state.getHeading()))
                / Math.cos(Math.toRadians(state.getLatitude()));

        double newLat = state.getLatitude() + deltaLat;
        double newLon = state.getLongitude() + deltaLon;

        // Check bounds and bounce back or change direction
        if (newLat < cityBoundsMinLat || newLat > cityBoundsMaxLat ||
            newLon < cityBoundsMinLon || newLon > cityBoundsMaxLon) {
            // Change direction when hitting city bounds
            state.setHeading((state.getHeading() + 180 + ThreadLocalRandom.current().nextDouble(-45, 45)) % 360);
            newLat = Math.max(cityBoundsMinLat, Math.min(cityBoundsMaxLat, newLat));
            newLon = Math.max(cityBoundsMinLon, Math.min(cityBoundsMaxLon, newLon));
        } else {
            // Occasionally change direction slightly for realistic movement
            if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                state.setHeading((state.getHeading() + ThreadLocalRandom.current().nextDouble(-30, 30) + 360) % 360);
            }
        }

        state.setLatitude(newLat);
        state.setLongitude(newLon);

        // Update destination if reached or randomly
        if (state.hasReachedDestination() || ThreadLocalRandom.current().nextDouble() < 0.05) {
            state.setDestination(randomLatitude(), randomLongitude());
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
    private Mono<CarLocation> createLocationUpdate(Car car, SimulatedCarState state,
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

    private double randomLatitude() {
        return cityBoundsMinLat + ThreadLocalRandom.current().nextDouble() * 
                (cityBoundsMaxLat - cityBoundsMinLat);
    }

    private double randomLongitude() {
        return cityBoundsMinLon + ThreadLocalRandom.current().nextDouble() * 
                (cityBoundsMaxLon - cityBoundsMinLon);
    }

    private String generateLicensePlate() {
        return String.format("%s-%d", 
                (char) ('A' + ThreadLocalRandom.current().nextInt(26)) +
                (char) ('A' + ThreadLocalRandom.current().nextInt(26)),
                ThreadLocalRandom.current().nextInt(100, 999));
    }

    private String generateRandomMake() {
        String[] makes = {"Toyota", "Honda", "Ford", "BMW", "Mercedes", "Audi", "Volkswagen", "Nissan", "Hyundai", "Kia"};
        return makes[ThreadLocalRandom.current().nextInt(makes.length)];
    }

    private String generateRandomModel() {
        String[] models = {"Sedan", "SUV", "Hatchback", "Coupe", "Convertible", "Truck"};
        return models[ThreadLocalRandom.current().nextInt(models.length)];
    }

    private String generateRandomColor() {
        String[] colors = {"Red", "Blue", "Black", "White", "Silver", "Gray", "Green", "Yellow"};
        return colors[ThreadLocalRandom.current().nextInt(colors.length)];
    }

    /**
     * Internal state for simulated car position
     */
    private class SimulatedCarState {
        private double latitude;
        private double longitude;
        private double heading;
        private double currentSpeed;
        private double destinationLat;
        private double destinationLon;

        public SimulatedCarState(double lat, double lon, double heading) {
            this.latitude = lat;
            this.longitude = lon;
            this.heading = heading;
            this.currentSpeed = baseSpeedKmh;
            this.destinationLat = randomLatitude();
            this.destinationLon = randomLongitude();
        }

        public boolean hasReachedDestination() {
            double distance = calculateDistance(latitude, longitude, destinationLat, destinationLon);
            return distance < 0.1; // Within 100 meters
        }

        public void setDestination(double lat, double lon) {
            this.destinationLat = lat;
            this.destinationLon = lon;
            // Update heading towards destination
            this.heading = calculateBearing(latitude, longitude, lat, lon);
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

        // Getters and Setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public double getHeading() { return heading; }
        public void setHeading(double heading) { this.heading = heading; }

        public double getCurrentSpeed() { return currentSpeed; }
        public void setCurrentSpeed(double currentSpeed) { this.currentSpeed = currentSpeed; }
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

