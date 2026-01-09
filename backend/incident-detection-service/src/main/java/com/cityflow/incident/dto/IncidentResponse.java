package com.cityflow.incident.dto;

import com.cityflow.incident.model.IncidentSeverity;
import com.cityflow.incident.model.IncidentStatus;
import com.cityflow.incident.model.IncidentType;

import java.time.Instant;
import java.util.List;

public class IncidentResponse {
    
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
    private Instant createdAt;
    private Instant updatedAt;

    public IncidentResponse() {
    }

    public static IncidentResponseBuilder builder() {
        return new IncidentResponseBuilder();
    }

    public static class IncidentResponseBuilder {
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
        private Instant createdAt;
        private Instant updatedAt;

        public IncidentResponseBuilder id(String id) { this.id = id; return this; }
        public IncidentResponseBuilder incidentCode(String incidentCode) { this.incidentCode = incidentCode; return this; }
        public IncidentResponseBuilder type(IncidentType type) { this.type = type; return this; }
        public IncidentResponseBuilder severity(IncidentSeverity severity) { this.severity = severity; return this; }
        public IncidentResponseBuilder status(IncidentStatus status) { this.status = status; return this; }
        public IncidentResponseBuilder title(String title) { this.title = title; return this; }
        public IncidentResponseBuilder description(String description) { this.description = description; return this; }
        public IncidentResponseBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public IncidentResponseBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public IncidentResponseBuilder roadSegmentId(String roadSegmentId) { this.roadSegmentId = roadSegmentId; return this; }
        public IncidentResponseBuilder address(String address) { this.address = address; return this; }
        public IncidentResponseBuilder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public IncidentResponseBuilder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public IncidentResponseBuilder relatedSourceIds(List<String> relatedSourceIds) { this.relatedSourceIds = relatedSourceIds; return this; }
        public IncidentResponseBuilder detectedAt(Instant detectedAt) { this.detectedAt = detectedAt; return this; }
        public IncidentResponseBuilder confirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; return this; }
        public IncidentResponseBuilder resolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; return this; }
        public IncidentResponseBuilder detectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; return this; }
        public IncidentResponseBuilder confidence(Double confidence) { this.confidence = confidence; return this; }
        public IncidentResponseBuilder impactRadiusKm(Double impactRadiusKm) { this.impactRadiusKm = impactRadiusKm; return this; }
        public IncidentResponseBuilder affectedVehicles(Integer affectedVehicles) { this.affectedVehicles = affectedVehicles; return this; }
        public IncidentResponseBuilder affectedBuses(Integer affectedBuses) { this.affectedBuses = affectedBuses; return this; }
        public IncidentResponseBuilder estimatedDelayMinutes(Integer estimatedDelayMinutes) { this.estimatedDelayMinutes = estimatedDelayMinutes; return this; }
        public IncidentResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public IncidentResponseBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public IncidentResponse build() {
            IncidentResponse response = new IncidentResponse();
            response.id = this.id;
            response.incidentCode = this.incidentCode;
            response.type = this.type;
            response.severity = this.severity;
            response.status = this.status;
            response.title = this.title;
            response.description = this.description;
            response.latitude = this.latitude;
            response.longitude = this.longitude;
            response.roadSegmentId = this.roadSegmentId;
            response.address = this.address;
            response.sourceId = this.sourceId;
            response.sourceType = this.sourceType;
            response.relatedSourceIds = this.relatedSourceIds;
            response.detectedAt = this.detectedAt;
            response.confirmedAt = this.confirmedAt;
            response.resolvedAt = this.resolvedAt;
            response.detectionMethod = this.detectionMethod;
            response.confidence = this.confidence;
            response.impactRadiusKm = this.impactRadiusKm;
            response.affectedVehicles = this.affectedVehicles;
            response.affectedBuses = this.affectedBuses;
            response.estimatedDelayMinutes = this.estimatedDelayMinutes;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
