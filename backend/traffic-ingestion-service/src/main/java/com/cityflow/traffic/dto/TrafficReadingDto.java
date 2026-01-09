package com.cityflow.traffic.dto;

import com.cityflow.traffic.model.CongestionLevel;
import com.cityflow.traffic.model.TrafficReading;

import java.time.Instant;

public class TrafficReadingDto {

    private String id;
    private String sensorId;
    private String sensorCode;
    private String roadSegmentId;
    private Instant timestamp;
    private Double averageSpeed;
    private Integer vehicleCount;
    private Double occupancy;
    private CongestionLevel congestionLevel;
    private Integer queueLength;
    private Double temperature;
    private String weatherCondition;
    private Boolean incidentDetected;
    private Instant createdAt;

    public TrafficReadingDto() {}

    public static TrafficReadingDto fromEntity(TrafficReading reading) {
        TrafficReadingDto dto = new TrafficReadingDto();
        dto.setId(reading.getId());
        dto.setSensorId(reading.getSensorId());
        dto.setSensorCode(reading.getSensorCode());
        dto.setRoadSegmentId(reading.getRoadSegmentId());
        dto.setTimestamp(reading.getTimestamp());
        dto.setAverageSpeed(reading.getAverageSpeed());
        dto.setVehicleCount(reading.getVehicleCount());
        dto.setOccupancy(reading.getOccupancy());
        dto.setCongestionLevel(reading.getCongestionLevel());
        dto.setQueueLength(reading.getQueueLength());
        dto.setTemperature(reading.getTemperature());
        dto.setWeatherCondition(reading.getWeatherCondition());
        dto.setIncidentDetected(reading.getIncidentDetected());
        dto.setCreatedAt(reading.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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

    public CongestionLevel getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(CongestionLevel congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public Integer getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(Integer queueLength) {
        this.queueLength = queueLength;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public Boolean getIncidentDetected() {
        return incidentDetected;
    }

    public void setIncidentDetected(Boolean incidentDetected) {
        this.incidentDetected = incidentDetected;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
