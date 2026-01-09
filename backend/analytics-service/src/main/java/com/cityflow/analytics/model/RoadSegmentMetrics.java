package com.cityflow.analytics.model;

import java.time.Instant;

public class RoadSegmentMetrics {
    
    private String roadSegmentId;
    private Instant timestamp;
    
    // Traffic metrics
    private Double averageSpeed;
    private Integer totalVehicles;
    private Double averageOccupancy;
    private String congestionLevel;
    private Integer queueLength;
    
    // Bus metrics
    private Integer activeBusCount;
    private Double averageBusDelay;  // minutes
    
    // Calculated metrics
    private Double trafficFlowScore;  // 0-100
    private Integer incidentCount;

    public RoadSegmentMetrics() {
        this.timestamp = Instant.now();
    }

    public RoadSegmentMetrics(String roadSegmentId) {
        this();
        this.roadSegmentId = roadSegmentId;
    }

    // Getters and Setters
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

    public Integer getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(Integer totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public Double getAverageOccupancy() {
        return averageOccupancy;
    }

    public void setAverageOccupancy(Double averageOccupancy) {
        this.averageOccupancy = averageOccupancy;
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

    public Integer getActiveBusCount() {
        return activeBusCount;
    }

    public void setActiveBusCount(Integer activeBusCount) {
        this.activeBusCount = activeBusCount;
    }

    public Double getAverageBusDelay() {
        return averageBusDelay;
    }

    public void setAverageBusDelay(Double averageBusDelay) {
        this.averageBusDelay = averageBusDelay;
    }

    public Double getTrafficFlowScore() {
        return trafficFlowScore;
    }

    public void setTrafficFlowScore(Double trafficFlowScore) {
        this.trafficFlowScore = trafficFlowScore;
    }

    public Integer getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(Integer incidentCount) {
        this.incidentCount = incidentCount;
    }
}
