package com.cityflow.traffic.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.Instant;

@Document(collection = "traffic_readings")
@CompoundIndexes({
    @CompoundIndex(name = "sensor_timestamp", def = "{'sensorId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "roadSegment_timestamp", def = "{'roadSegmentId': 1, 'timestamp': -1}")
})
public class TrafficReading {

    @Id
    private String id;

    @Indexed
    private String sensorId;

    private String sensorCode;

    @Indexed
    private String roadSegmentId;

    @Indexed
    private Instant timestamp;

    // Traffic metrics
    private Double averageSpeed;        // km/h
    private Integer vehicleCount;       // vehicles in time window
    private Double occupancy;           // % of time road is occupied
    private CongestionLevel congestionLevel;
    private Integer queueLength;        // vehicles waiting
    
    // Additional data
    private Double temperature;         // environmental data
    private String weatherCondition;    // CLEAR, RAIN, FOG, SNOW
    private Boolean incidentDetected;   // any incident flag
    
    private Instant createdAt;

    public TrafficReading() {
        this.timestamp = Instant.now();
        this.createdAt = Instant.now();
        this.incidentDetected = false;
    }

    public TrafficReading(String sensorId, String sensorCode, String roadSegmentId) {
        this();
        this.sensorId = sensorId;
        this.sensorCode = sensorCode;
        this.roadSegmentId = roadSegmentId;
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
