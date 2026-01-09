package com.cityflow.analytics.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CityWideMetrics {
    
    private Instant timestamp;
    
    // Overall statistics
    private Integer totalActiveBuses;
    private Integer totalActiveSensors;
    private Integer totalRoadSegments;
    
    // Traffic statistics
    private Double cityAverageSpeed;
    private Integer totalVehiclesDetected;
    private Map<String, Integer> congestionLevelCounts;  // FREE_FLOW: 10, MODERATE: 5, etc.
    
    // Bus statistics
    private Integer busesOnTime;
    private Integer busesDelayed;
    private Double averageDelay;  // minutes
    
    // Incidents
    private Integer activeIncidents;
    
    // Performance score
    private Double cityTrafficScore;  // 0-100

    public CityWideMetrics() {
        this.timestamp = Instant.now();
        this.congestionLevelCounts = new HashMap<>();
    }

    // Getters and Setters
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTotalActiveBuses() {
        return totalActiveBuses;
    }

    public void setTotalActiveBuses(Integer totalActiveBuses) {
        this.totalActiveBuses = totalActiveBuses;
    }

    public Integer getTotalActiveSensors() {
        return totalActiveSensors;
    }

    public void setTotalActiveSensors(Integer totalActiveSensors) {
        this.totalActiveSensors = totalActiveSensors;
    }

    public Integer getTotalRoadSegments() {
        return totalRoadSegments;
    }

    public void setTotalRoadSegments(Integer totalRoadSegments) {
        this.totalRoadSegments = totalRoadSegments;
    }

    public Double getCityAverageSpeed() {
        return cityAverageSpeed;
    }

    public void setCityAverageSpeed(Double cityAverageSpeed) {
        this.cityAverageSpeed = cityAverageSpeed;
    }

    public Integer getTotalVehiclesDetected() {
        return totalVehiclesDetected;
    }

    public void setTotalVehiclesDetected(Integer totalVehiclesDetected) {
        this.totalVehiclesDetected = totalVehiclesDetected;
    }

    public Map<String, Integer> getCongestionLevelCounts() {
        return congestionLevelCounts;
    }

    public void setCongestionLevelCounts(Map<String, Integer> congestionLevelCounts) {
        this.congestionLevelCounts = congestionLevelCounts;
    }

    public Integer getBusesOnTime() {
        return busesOnTime;
    }

    public void setBusesOnTime(Integer busesOnTime) {
        this.busesOnTime = busesOnTime;
    }

    public Integer getBusesDelayed() {
        return busesDelayed;
    }

    public void setBusesDelayed(Integer busesDelayed) {
        this.busesDelayed = busesDelayed;
    }

    public Double getAverageDelay() {
        return averageDelay;
    }

    public void setAverageDelay(Double averageDelay) {
        this.averageDelay = averageDelay;
    }

    public Integer getActiveIncidents() {
        return activeIncidents;
    }

    public void setActiveIncidents(Integer activeIncidents) {
        this.activeIncidents = activeIncidents;
    }

    public Double getCityTrafficScore() {
        return cityTrafficScore;
    }

    public void setCityTrafficScore(Double cityTrafficScore) {
        this.cityTrafficScore = cityTrafficScore;
    }
}
