package com.cityflow.incident.event;

import com.cityflow.incident.model.IncidentSeverity;
import com.cityflow.incident.model.IncidentStatus;
import com.cityflow.incident.model.IncidentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published to Kafka when an incident is detected or updated
 */
public class IncidentEvent {
    
    private String eventId;
    private String incidentId;
    private String incidentCode;
    private IncidentType type;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;
    private String roadSegmentId;
    private String sourceId;
    private String sourceType;
    private Instant detectedAt;
    private Double confidence;
    private Integer affectedVehicles;
    private Integer affectedBuses;
    private Instant eventTimestamp;

    public IncidentEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventTimestamp = Instant.now();
    }

    public static IncidentEventBuilder builder() {
        return new IncidentEventBuilder();
    }

    public static class IncidentEventBuilder {
        private String eventId;
        private String incidentId;
        private String incidentCode;
        private IncidentType type;
        private IncidentSeverity severity;
        private IncidentStatus status;
        private String title;
        private String description;
        private Double latitude;
        private Double longitude;
        private String roadSegmentId;
        private String sourceId;
        private String sourceType;
        private Instant detectedAt;
        private Double confidence;
        private Integer affectedVehicles;
        private Integer affectedBuses;
        private Instant eventTimestamp;

        public IncidentEventBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public IncidentEventBuilder incidentId(String incidentId) { this.incidentId = incidentId; return this; }
        public IncidentEventBuilder incidentCode(String incidentCode) { this.incidentCode = incidentCode; return this; }
        public IncidentEventBuilder type(IncidentType type) { this.type = type; return this; }
        public IncidentEventBuilder severity(IncidentSeverity severity) { this.severity = severity; return this; }
        public IncidentEventBuilder status(IncidentStatus status) { this.status = status; return this; }
        public IncidentEventBuilder title(String title) { this.title = title; return this; }
        public IncidentEventBuilder description(String description) { this.description = description; return this; }
        public IncidentEventBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public IncidentEventBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public IncidentEventBuilder roadSegmentId(String roadSegmentId) { this.roadSegmentId = roadSegmentId; return this; }
        public IncidentEventBuilder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public IncidentEventBuilder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public IncidentEventBuilder detectedAt(Instant detectedAt) { this.detectedAt = detectedAt; return this; }
        public IncidentEventBuilder confidence(Double confidence) { this.confidence = confidence; return this; }
        public IncidentEventBuilder affectedVehicles(Integer affectedVehicles) { this.affectedVehicles = affectedVehicles; return this; }
        public IncidentEventBuilder affectedBuses(Integer affectedBuses) { this.affectedBuses = affectedBuses; return this; }
        public IncidentEventBuilder eventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; return this; }

        public IncidentEvent build() {
            IncidentEvent event = new IncidentEvent();
            event.eventId = this.eventId != null ? this.eventId : UUID.randomUUID().toString();
            event.incidentId = this.incidentId;
            event.incidentCode = this.incidentCode;
            event.type = this.type;
            event.severity = this.severity;
            event.status = this.status;
            event.title = this.title;
            event.description = this.description;
            event.latitude = this.latitude;
            event.longitude = this.longitude;
            event.roadSegmentId = this.roadSegmentId;
            event.sourceId = this.sourceId;
            event.sourceType = this.sourceType;
            event.detectedAt = this.detectedAt;
            event.confidence = this.confidence;
            event.affectedVehicles = this.affectedVehicles;
            event.affectedBuses = this.affectedBuses;
            event.eventTimestamp = this.eventTimestamp != null ? this.eventTimestamp : Instant.now();
            return event;
        }
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

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

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Integer getAffectedVehicles() { return affectedVehicles; }
    public void setAffectedVehicles(Integer affectedVehicles) { this.affectedVehicles = affectedVehicles; }

    public Integer getAffectedBuses() { return affectedBuses; }
    public void setAffectedBuses(Integer affectedBuses) { this.affectedBuses = affectedBuses; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
}
