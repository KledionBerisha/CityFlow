package com.cityflow.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrafficReadingEvent {
    private String sensorId;
    private String sensorCode;
    private String roadSegmentId;
    private String congestionLevel;
    private Boolean incidentDetected;
    private Double averageSpeed;
    private Integer vehicleCount;
    private Instant timestamp;

    // Getters and Setters
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    
    public String getSensorCode() { return sensorCode; }
    public void setSensorCode(String sensorCode) { this.sensorCode = sensorCode; }
    
    public String getRoadSegmentId() { return roadSegmentId; }
    public void setRoadSegmentId(String roadSegmentId) { this.roadSegmentId = roadSegmentId; }
    
    public String getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(String congestionLevel) { this.congestionLevel = congestionLevel; }
    
    public Boolean getIncidentDetected() { return incidentDetected; }
    public void setIncidentDetected(Boolean incidentDetected) { this.incidentDetected = incidentDetected; }
    
    public Double getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(Double averageSpeed) { this.averageSpeed = averageSpeed; }
    
    public Integer getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(Integer vehicleCount) { this.vehicleCount = vehicleCount; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
