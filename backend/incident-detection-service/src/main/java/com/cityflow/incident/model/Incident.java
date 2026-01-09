package com.cityflow.incident.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "incidents")
@CompoundIndex(name = "location_timestamp_idx", def = "{'latitude': 1, 'longitude': 1, 'detectedAt': -1}")
@CompoundIndex(name = "type_severity_idx", def = "{'type': 1, 'severity': 1, 'detectedAt': -1}")
public class Incident {

    @Id
    private String id;

    @Indexed
    private String incidentCode;  // e.g., "INC-2026-0001"

    private IncidentType type;

    private IncidentSeverity severity;

    private IncidentStatus status;

    private String title;

    private String description;

    // Location
    private Double latitude;
    private Double longitude;
    private String roadSegmentId;
    private String address;

    // Source information
    private String sourceId;  // sensor ID, bus ID, etc.
    private String sourceType;  // "TRAFFIC_SENSOR", "BUS", "MANUAL"
    private List<String> relatedSourceIds;  // correlated sources

    // Detection details
    @Indexed
    private Instant detectedAt;
    private Instant confirmedAt;
    private Instant resolvedAt;
    
    private String detectionMethod;  // algorithm used
    private Double confidence;  // 0.0 to 1.0

    // Impact metrics
    private Double impactRadiusKm;
    private Integer affectedVehicles;
    private Integer affectedBuses;
    private Integer estimatedDelayMinutes;

    // Event data
    private IncidentMetadata metadata;

    @Indexed
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public Incident() {
    }

    public static IncidentBuilder builder() {
        return new IncidentBuilder();
    }

    public static class IncidentBuilder {
        private String id;
        private String incidentCode;
        private IncidentType type;
        private IncidentSeverity severity;
        private IncidentStatus status;
        private String title;
        private String description;
        private Double latitude;
        private Double longitude;
        private String roadSegmentId;
        private String address;
        private String sourceId;
        private String sourceType;
        private List<String> relatedSourceIds;
        private Instant detectedAt;
        private Instant confirmedAt;
        private Instant resolvedAt;
        private String detectionMethod;
        private Double confidence;
        private Double impactRadiusKm;
        private Integer affectedVehicles;
        private Integer affectedBuses;
        private Integer estimatedDelayMinutes;
        private IncidentMetadata metadata;
        private Instant createdAt;
        private Instant updatedAt;

        public IncidentBuilder id(String id) { this.id = id; return this; }
        public IncidentBuilder incidentCode(String incidentCode) { this.incidentCode = incidentCode; return this; }
        public IncidentBuilder type(IncidentType type) { this.type = type; return this; }
        public IncidentBuilder severity(IncidentSeverity severity) { this.severity = severity; return this; }
        public IncidentBuilder status(IncidentStatus status) { this.status = status; return this; }
        public IncidentBuilder title(String title) { this.title = title; return this; }
        public IncidentBuilder description(String description) { this.description = description; return this; }
        public IncidentBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public IncidentBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public IncidentBuilder roadSegmentId(String roadSegmentId) { this.roadSegmentId = roadSegmentId; return this; }
        public IncidentBuilder address(String address) { this.address = address; return this; }
        public IncidentBuilder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public IncidentBuilder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public IncidentBuilder relatedSourceIds(List<String> relatedSourceIds) { this.relatedSourceIds = relatedSourceIds; return this; }
        public IncidentBuilder detectedAt(Instant detectedAt) { this.detectedAt = detectedAt; return this; }
        public IncidentBuilder confirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; return this; }
        public IncidentBuilder resolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; return this; }
        public IncidentBuilder detectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; return this; }
        public IncidentBuilder confidence(Double confidence) { this.confidence = confidence; return this; }
        public IncidentBuilder impactRadiusKm(Double impactRadiusKm) { this.impactRadiusKm = impactRadiusKm; return this; }
        public IncidentBuilder affectedVehicles(Integer affectedVehicles) { this.affectedVehicles = affectedVehicles; return this; }
        public IncidentBuilder affectedBuses(Integer affectedBuses) { this.affectedBuses = affectedBuses; return this; }
        public IncidentBuilder estimatedDelayMinutes(Integer estimatedDelayMinutes) { this.estimatedDelayMinutes = estimatedDelayMinutes; return this; }
        public IncidentBuilder metadata(IncidentMetadata metadata) { this.metadata = metadata; return this; }
        public IncidentBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public IncidentBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Incident build() {
            Incident incident = new Incident();
            incident.id = this.id;
            incident.incidentCode = this.incidentCode;
            incident.type = this.type;
            incident.severity = this.severity;
            incident.status = this.status;
            incident.title = this.title;
            incident.description = this.description;
            incident.latitude = this.latitude;
            incident.longitude = this.longitude;
            incident.roadSegmentId = this.roadSegmentId;
            incident.address = this.address;
            incident.sourceId = this.sourceId;
            incident.sourceType = this.sourceType;
            incident.relatedSourceIds = this.relatedSourceIds;
            incident.detectedAt = this.detectedAt;
            incident.confirmedAt = this.confirmedAt;
            incident.resolvedAt = this.resolvedAt;
            incident.detectionMethod = this.detectionMethod;
            incident.confidence = this.confidence;
            incident.impactRadiusKm = this.impactRadiusKm;
            incident.affectedVehicles = this.affectedVehicles;
            incident.affectedBuses = this.affectedBuses;
            incident.estimatedDelayMinutes = this.estimatedDelayMinutes;
            incident.metadata = this.metadata;
            incident.createdAt = this.createdAt;
            incident.updatedAt = this.updatedAt;
            return incident;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentCode() { return incidentCode; }
    public void setIncidentCode(String incidentCode) { this.incidentCode = incidentCode; }

    public IncidentType getType() { return type; }
    public void setType(IncidentType type) { this.type = type; }

    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getRoadSegmentId() { return roadSegmentId; }
    public void setRoadSegmentId(String roadSegmentId) { this.roadSegmentId = roadSegmentId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public List<String> getRelatedSourceIds() { return relatedSourceIds; }
    public void setRelatedSourceIds(List<String> relatedSourceIds) { this.relatedSourceIds = relatedSourceIds; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Double getImpactRadiusKm() { return impactRadiusKm; }
    public void setImpactRadiusKm(Double impactRadiusKm) { this.impactRadiusKm = impactRadiusKm; }

    public Integer getAffectedVehicles() { return affectedVehicles; }
    public void setAffectedVehicles(Integer affectedVehicles) { this.affectedVehicles = affectedVehicles; }

    public Integer getAffectedBuses() { return affectedBuses; }
    public void setAffectedBuses(Integer affectedBuses) { this.affectedBuses = affectedBuses; }

    public Integer getEstimatedDelayMinutes() { return estimatedDelayMinutes; }
    public void setEstimatedDelayMinutes(Integer estimatedDelayMinutes) { this.estimatedDelayMinutes = estimatedDelayMinutes; }

    public IncidentMetadata getMetadata() { return metadata; }
    public void setMetadata(IncidentMetadata metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
