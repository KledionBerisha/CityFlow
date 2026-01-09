package com.cityflow.incident.dto;

import java.time.Instant;

/**
 * Event consumed from traffic-ingestion-service
 */
public class TrafficReadingEvent {
    
    private String sensorId;
    private String sensorCode;
    private String roadSegmentId;
    private Double latitude;
    private Double longitude;
    private Double averageSpeedKmh;
    private Integer vehicleCount;
    private Double occupancy;
    private String congestionLevel;
    private Integer queueLength;
    private Boolean incidentDetected;
    private Double temperatureCelsius;
    private String weatherCondition;
    private Instant timestamp;

    public TrafficReadingEvent() {
    }

    // Getters and Setters
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getSensorCode() { return sensorCode; }
    public void setSensorCode(String sensorCode) { this.sensorCode = sensorCode; }

    public String getRoadSegmentId() { return roadSegmentId; }
    public void setRoadSegmentId(String roadSegmentId) { this.roadSegmentId = roadSegmentId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAverageSpeedKmh() { return averageSpeedKmh; }
    public void setAverageSpeedKmh(Double averageSpeedKmh) { this.averageSpeedKmh = averageSpeedKmh; }

    public Integer getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(Integer vehicleCount) { this.vehicleCount = vehicleCount; }

    public Double getOccupancy() { return occupancy; }
    public void setOccupancy(Double occupancy) { this.occupancy = occupancy; }

    public String getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(String congestionLevel) { this.congestionLevel = congestionLevel; }

    public Integer getQueueLength() { return queueLength; }
    public void setQueueLength(Integer queueLength) { this.queueLength = queueLength; }

    public Boolean getIncidentDetected() { return incidentDetected; }
    public void setIncidentDetected(Boolean incidentDetected) { this.incidentDetected = incidentDetected; }

    public Double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(Double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
