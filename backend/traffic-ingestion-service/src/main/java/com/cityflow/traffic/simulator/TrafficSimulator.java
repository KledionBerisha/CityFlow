package com.cityflow.traffic.simulator;

import com.cityflow.traffic.model.CongestionLevel;
import com.cityflow.traffic.model.Sensor;
import com.cityflow.traffic.model.SensorStatus;
import com.cityflow.traffic.model.TrafficReading;
import com.cityflow.traffic.repository.SensorRepository;
import com.cityflow.traffic.service.TrafficReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Random;

@Component
public class TrafficSimulator {

    private static final Logger log = LoggerFactory.getLogger(TrafficSimulator.class);
    private static final Random random = new Random();
    private static final String[] WEATHER_CONDITIONS = {"CLEAR", "RAIN", "FOG", "CLOUDY"};

    private final SensorRepository sensorRepository;
    private final TrafficReadingService trafficReadingService;
    private final boolean simulatorEnabled;

    public TrafficSimulator(
            SensorRepository sensorRepository,
            TrafficReadingService trafficReadingService,
            @Value("${app.simulator.enabled:true}") boolean simulatorEnabled) {
        this.sensorRepository = sensorRepository;
        this.trafficReadingService = trafficReadingService;
        this.simulatorEnabled = simulatorEnabled;
    }

    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:10000}")
    public void simulateTrafficReadings() {
        if (!simulatorEnabled) {
            return;
        }

        sensorRepository.findByStatus(SensorStatus.ACTIVE)
                .flatMap(this::generateReading)
                .flatMap(trafficReadingService::recordReading)
                .subscribe(
                        reading -> log.debug("Simulated traffic reading for sensor {}", reading.getSensorCode()),
                        error -> log.error("Error simulating traffic readings", error)
                );
    }

    private Flux<TrafficReading> generateReading(Sensor sensor) {
        TrafficReading reading = new TrafficReading(
                sensor.getId(), 
                sensor.getCode(), 
                sensor.getRoadSegmentId()
        );

        // Simulate traffic based on time of day
        int hour = Instant.now().atZone(java.time.ZoneId.systemDefault()).getHour();
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
        boolean isNightTime = hour >= 22 || hour <= 5;

        // Generate speed (km/h)
        double speedLimit = sensor.getSpeedLimit() != null ? sensor.getSpeedLimit() : 50.0;
        double averageSpeed;
        int vehicleCount;
        double occupancy;
        CongestionLevel congestionLevel;

        if (isNightTime) {
            // Light traffic at night
            averageSpeed = speedLimit * (0.9 + random.nextDouble() * 0.1); // 90-100% of limit
            vehicleCount = random.nextInt(5) + 1;
            occupancy = random.nextDouble() * 0.2; // 0-20%
            congestionLevel = CongestionLevel.FREE_FLOW;
        } else if (isPeakHour) {
            // Heavy traffic during peak hours
            double congestionFactor = random.nextDouble();
            if (congestionFactor < 0.3) {
                // Severe congestion (30% chance in peak hours)
                averageSpeed = speedLimit * (0.1 + random.nextDouble() * 0.2); // 10-30%
                vehicleCount = 80 + random.nextInt(40);
                occupancy = 0.85 + random.nextDouble() * 0.15; // 85-100%
                congestionLevel = CongestionLevel.SEVERE;
            } else if (congestionFactor < 0.6) {
                // Heavy congestion
                averageSpeed = speedLimit * (0.25 + random.nextDouble() * 0.25); // 25-50%
                vehicleCount = 50 + random.nextInt(30);
                occupancy = 0.65 + random.nextDouble() * 0.2; // 65-85%
                congestionLevel = CongestionLevel.HEAVY;
            } else {
                // Moderate congestion
                averageSpeed = speedLimit * (0.5 + random.nextDouble() * 0.2); // 50-70%
                vehicleCount = 30 + random.nextInt(20);
                occupancy = 0.45 + random.nextDouble() * 0.2; // 45-65%
                congestionLevel = CongestionLevel.MODERATE;
            }
        } else {
            // Normal traffic
            double trafficLevel = random.nextDouble();
            if (trafficLevel < 0.7) {
                // Free flow
                averageSpeed = speedLimit * (0.8 + random.nextDouble() * 0.2); // 80-100%
                vehicleCount = 10 + random.nextInt(15);
                occupancy = 0.15 + random.nextDouble() * 0.15; // 15-30%
                congestionLevel = CongestionLevel.FREE_FLOW;
            } else {
                // Light congestion
                averageSpeed = speedLimit * (0.6 + random.nextDouble() * 0.2); // 60-80%
                vehicleCount = 25 + random.nextInt(15);
                occupancy = 0.3 + random.nextDouble() * 0.2; // 30-50%
                congestionLevel = CongestionLevel.LIGHT;
            }
        }

        reading.setAverageSpeed(Math.round(averageSpeed * 10.0) / 10.0);
        reading.setVehicleCount(vehicleCount);
        reading.setOccupancy(Math.round(occupancy * 100.0) / 100.0);
        reading.setCongestionLevel(congestionLevel);

        // Queue length based on congestion
        int queueLength = switch (congestionLevel) {
            case FREE_FLOW -> 0;
            case LIGHT -> random.nextInt(3);
            case MODERATE -> 3 + random.nextInt(7);
            case HEAVY -> 10 + random.nextInt(15);
            case SEVERE -> 25 + random.nextInt(30);
        };
        reading.setQueueLength(queueLength);

        // Environmental data
        reading.setTemperature(15.0 + random.nextDouble() * 15.0); // 15-30°C
        reading.setWeatherCondition(WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)]);

        // Incident detection (5% chance during severe congestion)
        if (congestionLevel == CongestionLevel.SEVERE && random.nextDouble() < 0.05) {
            reading.setIncidentDetected(true);
        }

        return Flux.just(reading);
    }
}
