package com.cityflow.incident.service;

import com.cityflow.incident.dto.BusLocationEvent;
import com.cityflow.incident.dto.TrafficReadingEvent;
import com.cityflow.incident.event.IncidentEvent;
import com.cityflow.incident.model.*;
import com.cityflow.incident.repository.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IncidentDetectionService {

    private static final Logger log = LoggerFactory.getLogger(IncidentDetectionService.class);

    private final IncidentRepository incidentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.incident-events:incident.events}")
    private String incidentEventsTopic;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    // In-memory buffers for pattern analysis
    private final Map<String, List<TrafficReadingEvent>> trafficHistory = new ConcurrentHashMap<>();
    private final Map<String, BusLocationEvent> lastBusPositions = new ConcurrentHashMap<>();
    private final Map<String, Instant> recentIncidents = new ConcurrentHashMap<>();

    public IncidentDetectionService(IncidentRepository incidentRepository,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyze traffic reading for potential incidents
     */
    public Mono<Void> analyzeTrafficReading(TrafficReadingEvent event) {
        return Mono.fromRunnable(() -> {
            try {
                // Update history buffer
                String sensorId = event.getSensorId();
                trafficHistory.computeIfAbsent(sensorId, k -> Collections.synchronizedList(new ArrayList<>()));
                List<TrafficReadingEvent> history = trafficHistory.get(sensorId);
                
                synchronized (history) {
                    history.add(event);
                    if (history.size() > 50) {
                        history.remove(0);  // Keep last 50 readings
                    }
                }

                // Run detection algorithms
                detectSuddenSpeedDrop(event, history);
                detectSevereCongestion(event);
                detectSensorIncident(event);

            } catch (Exception e) {
                log.error("Error analyzing traffic reading: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Analyze bus location for potential incidents
     */
    public Mono<Void> analyzeBusLocation(BusLocationEvent event) {
        return Mono.fromRunnable(() -> {
            try {
                String busId = event.getBusId();
                BusLocationEvent lastPosition = lastBusPositions.get(busId);
                
                if (lastPosition != null) {
                    detectBusAbnormality(event, lastPosition);
                }
                
                lastBusPositions.put(busId, event);

            } catch (Exception e) {
                log.error("Error analyzing bus location: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Detect sudden speed drop (potential accident)
     */
    private void detectSuddenSpeedDrop(TrafficReadingEvent current, List<TrafficReadingEvent> history) {
        if (history.size() < 5) return;

        double avgSpeedBefore = 0.0;
        int count = 0;
        
        synchronized (history) {
            for (int i = Math.max(0, history.size() - 6); i < history.size() - 1; i++) {
                avgSpeedBefore += history.get(i).getAverageSpeedKmh();
                count++;
            }
        }
        
        if (count == 0) return;
        avgSpeedBefore /= count;

        double currentSpeed = current.getAverageSpeedKmh();
        double speedDrop = ((avgSpeedBefore - currentSpeed) / avgSpeedBefore) * 100.0;

        // If speed drops more than 50% suddenly and current speed is very low
        if (speedDrop > 50 && currentSpeed < 20 && avgSpeedBefore > 40) {
            
            // Check if we already reported incident for this sensor recently
            String key = "speed_drop_" + current.getSensorId();
            Instant lastReport = recentIncidents.get(key);
            if (lastReport != null && Instant.now().minus(10, ChronoUnit.MINUTES).isBefore(lastReport)) {
                return;  // Already reported recently
            }
            
            log.warn("⚠️ INCIDENT DETECTED: Sudden speed drop at sensor {} ({} → {} km/h, drop: {}%)",
                    current.getSensorCode(), Math.round(avgSpeedBefore), Math.round(currentSpeed), Math.round(speedDrop));

            IncidentMetadata metadata = new IncidentMetadata();
            metadata.setAverageSpeedBeforeKmh(avgSpeedBefore);
            metadata.setAverageSpeedDuringKmh(currentSpeed);
            metadata.setSpeedDropPercentage(speedDrop);
            metadata.setWeatherCondition(current.getWeatherCondition());
            metadata.setTemperatureCelsius(current.getTemperatureCelsius());

            Incident incident = Incident.builder()
                    .incidentCode(generateIncidentCode())
                    .type(IncidentType.ACCIDENT)
                    .severity(speedDrop > 70 ? IncidentSeverity.CRITICAL : IncidentSeverity.HIGH)
                    .status(IncidentStatus.DETECTED)
                    .title("Sudden Speed Drop - Potential Accident")
                    .description(String.format("Sudden %d%% speed drop detected (%.1f → %.1f km/h)", 
                            (int)speedDrop, avgSpeedBefore, currentSpeed))
                    .latitude(current.getLatitude())
                    .longitude(current.getLongitude())
                    .roadSegmentId(current.getRoadSegmentId())
                    .sourceId(current.getSensorId())
                    .sourceType("TRAFFIC_SENSOR")
                    .detectedAt(current.getTimestamp())
                    .detectionMethod("SPEED_DROP_ANALYSIS")
                    .confidence(calculateConfidence(speedDrop, currentSpeed))
                    .impactRadiusKm(0.5)
                    .metadata(metadata)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            saveAndPublishIncident(incident);
            recentIncidents.put(key, Instant.now());
        }
    }

    /**
     * Detect severe congestion
     */
    private void detectSevereCongestion(TrafficReadingEvent event) {
        if ("SEVERE".equals(event.getCongestionLevel()) && event.getOccupancy() > 0.9) {
            
            String key = "congestion_" + event.getSensorId();
            Instant lastReport = recentIncidents.get(key);
            if (lastReport != null && Instant.now().minus(15, ChronoUnit.MINUTES).isBefore(lastReport)) {
                return;
            }

            log.warn("⚠️ INCIDENT DETECTED: Severe congestion at sensor {} (occupancy: {}%)",
                    event.getSensorCode(), Math.round(event.getOccupancy() * 100));

            Incident incident = Incident.builder()
                    .incidentCode(generateIncidentCode())
                    .type(IncidentType.SEVERE_CONGESTION)
                    .severity(IncidentSeverity.HIGH)
                    .status(IncidentStatus.DETECTED)
                    .title("Severe Traffic Congestion")
                    .description(String.format("Severe congestion with %.0f%% occupancy and %d vehicles", 
                            event.getOccupancy() * 100, event.getVehicleCount()))
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .roadSegmentId(event.getRoadSegmentId())
                    .sourceId(event.getSensorId())
                    .sourceType("TRAFFIC_SENSOR")
                    .detectedAt(event.getTimestamp())
                    .detectionMethod("CONGESTION_THRESHOLD")
                    .confidence(0.85)
                    .affectedVehicles(event.getVehicleCount())
                    .estimatedDelayMinutes(calculateEstimatedDelay(event))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            saveAndPublishIncident(incident);
            recentIncidents.put(key, Instant.now());
        }
    }

    /**
     * Detect sensor-reported incident
     */
    private void detectSensorIncident(TrafficReadingEvent event) {
        if (Boolean.TRUE.equals(event.getIncidentDetected())) {
            
            String key = "sensor_incident_" + event.getSensorId();
            Instant lastReport = recentIncidents.get(key);
            if (lastReport != null && Instant.now().minus(5, ChronoUnit.MINUTES).isBefore(lastReport)) {
                return;
            }

            log.warn("⚠️ INCIDENT DETECTED: Sensor {} flagged incident", event.getSensorCode());

            Incident incident = Incident.builder()
                    .incidentCode(generateIncidentCode())
                    .type(IncidentType.ACCIDENT)
                    .severity(IncidentSeverity.CRITICAL)
                    .status(IncidentStatus.DETECTED)
                    .title("Sensor-Detected Incident")
                    .description("Traffic sensor has flagged an incident requiring attention")
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .roadSegmentId(event.getRoadSegmentId())
                    .sourceId(event.getSensorId())
                    .sourceType("TRAFFIC_SENSOR")
                    .detectedAt(event.getTimestamp())
                    .detectionMethod("SENSOR_FLAG")
                    .confidence(0.95)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            saveAndPublishIncident(incident);
            recentIncidents.put(key, Instant.now());
        }
    }

    /**
     * Detect bus abnormalities (e.g., sudden stop)
     */
    private void detectBusAbnormality(BusLocationEvent current, BusLocationEvent previous) {
        // Check if bus suddenly stopped
        if (previous.getSpeedKmh() > 30 && current.getSpeedKmh() < 5) {
            long timeDiff = current.getTimestamp().getEpochSecond() - previous.getTimestamp().getEpochSecond();
            
            if (timeDiff < 30) {  // Sudden stop within 30 seconds
                String key = "bus_stop_" + current.getBusId();
                Instant lastReport = recentIncidents.get(key);
                if (lastReport != null && Instant.now().minus(10, ChronoUnit.MINUTES).isBefore(lastReport)) {
                    return;
                }

                log.warn("⚠️ INCIDENT DETECTED: Bus {} sudden stop ({} → {} km/h)",
                        current.getVehicleId(), Math.round(previous.getSpeedKmh()), Math.round(current.getSpeedKmh()));

                Incident incident = Incident.builder()
                        .incidentCode(generateIncidentCode())
                        .type(IncidentType.BUS_BREAKDOWN)
                        .severity(IncidentSeverity.MEDIUM)
                        .status(IncidentStatus.DETECTED)
                        .title("Bus Sudden Stop")
                        .description(String.format("Bus %s stopped suddenly (%.1f → %.1f km/h)", 
                                current.getVehicleId(), previous.getSpeedKmh(), current.getSpeedKmh()))
                        .latitude(current.getLatitude())
                        .longitude(current.getLongitude())
                        .sourceId(current.getBusId())
                        .sourceType("BUS")
                        .detectedAt(current.getTimestamp())
                        .detectionMethod("BUS_SPEED_ANOMALY")
                        .confidence(0.70)
                        .affectedBuses(1)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                saveAndPublishIncident(incident);
                recentIncidents.put(key, Instant.now());
            }
        }
    }

    /**
     * Save incident and publish to Kafka
     */
    private void saveAndPublishIncident(Incident incident) {
        incidentRepository.save(incident)
                .doOnSuccess(saved -> {
                    log.info("✅ Incident saved: {} ({})", saved.getIncidentCode(), saved.getType());
                    publishIncidentEvent(saved);
                })
                .doOnError(e -> log.error("❌ Failed to save incident: {}", e.getMessage()))
                .subscribe();
    }

    /**
     * Publish incident event to Kafka
     */
    private void publishIncidentEvent(Incident incident) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping incident event publish");
            return;
        }

        try {
            IncidentEvent event = IncidentEvent.builder()
                    .incidentId(incident.getId())
                    .incidentCode(incident.getIncidentCode())
                    .type(incident.getType())
                    .severity(incident.getSeverity())
                    .status(incident.getStatus())
                    .title(incident.getTitle())
                    .description(incident.getDescription())
                    .latitude(incident.getLatitude())
                    .longitude(incident.getLongitude())
                    .roadSegmentId(incident.getRoadSegmentId())
                    .sourceId(incident.getSourceId())
                    .sourceType(incident.getSourceType())
                    .detectedAt(incident.getDetectedAt())
                    .confidence(incident.getConfidence())
                    .affectedVehicles(incident.getAffectedVehicles())
                    .affectedBuses(incident.getAffectedBuses())
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(incidentEventsTopic, incident.getIncidentCode(), eventJson);
            log.info("📤 Published incident event: {} to topic {}", incident.getIncidentCode(), incidentEventsTopic);
            
        } catch (Exception e) {
            log.error("Failed to publish incident event: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate unique incident code
     */
    private String generateIncidentCode() {
        return String.format("INC-%s-%04d", 
                java.time.Year.now().getValue(), 
                new Random().nextInt(10000));
    }

    /**
     * Calculate confidence score based on detection parameters
     */
    private double calculateConfidence(double speedDrop, double currentSpeed) {
        double baseConfidence = 0.60;
        if (speedDrop > 70) baseConfidence += 0.20;
        else if (speedDrop > 60) baseConfidence += 0.15;
        if (currentSpeed < 10) baseConfidence += 0.10;
        return Math.min(0.95, baseConfidence);
    }

    /**
     * Estimate delay in minutes based on congestion
     */
    private int calculateEstimatedDelay(TrafficReadingEvent event) {
        if (event.getAverageSpeedKmh() < 10) return 15;
        if (event.getAverageSpeedKmh() < 20) return 10;
        return 5;
    }
}
