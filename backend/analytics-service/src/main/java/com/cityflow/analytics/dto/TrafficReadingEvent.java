package com.cityflow.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrafficReadingEvent {
    
    private String eventId;
    private String eventType;
    private Instant timestamp;
    private String sensorId;
    private String sensorCode;
    private String roadSegmentId;
    private Double averageSpeed;
    private Integer vehicleCount;
    private Double occupancy;
    private String congestionLevel;
    private Integer queueLength;
    private Boolean incidentDetected;

    public TrafficReadingEvent() {}

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public void setSensorCode(String sensorCode) {
        this.sensorCode = sensorCode;
    }

    public String getRoadSegmentId() {
        return roadSegmentId;
    }

    public void setRoadSegmentId(String roadSegmentId) {
        this.roadSegmentId = roadSegmentId;
    }

    public Double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public Integer getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(Integer vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public Double getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(Double occupancy) {
        this.occupancy = occupancy;
    }

    public String getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(String congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public Integer getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(Integer queueLength) {
        this.queueLength = queueLength;
    }

    public Boolean getIncidentDetected() {
        return incidentDetected;
    }

    public void setIncidentDetected(Boolean incidentDetected) {
        this.incidentDetected = incidentDetected;
    }
}
