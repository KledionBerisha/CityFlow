package com.cityflow.incident.model;

import java.util.Map;

/**
 * Additional metadata captured during incident detection
 */
public class IncidentMetadata {

    private Double averageSpeedBeforeKmh;
    private Double averageSpeedDuringKmh;
    private Double speedDropPercentage;
    
    private Integer vehicleCountBefore;
    private Integer vehicleCountDuring;
    
    private Double occupancyBefore;
    private Double occupancyDuring;
    
    private String weatherCondition;
    private Double temperatureCelsius;
    
    private Map<String, Object> additionalData;

    public IncidentMetadata() {
    }

    // Getters and Setters
    public Double getAverageSpeedBeforeKmh() { return averageSpeedBeforeKmh; }
    public void setAverageSpeedBeforeKmh(Double averageSpeedBeforeKmh) { this.averageSpeedBeforeKmh = averageSpeedBeforeKmh; }

    public Double getAverageSpeedDuringKmh() { return averageSpeedDuringKmh; }
    public void setAverageSpeedDuringKmh(Double averageSpeedDuringKmh) { this.averageSpeedDuringKmh = averageSpeedDuringKmh; }

    public Double getSpeedDropPercentage() { return speedDropPercentage; }
    public void setSpeedDropPercentage(Double speedDropPercentage) { this.speedDropPercentage = speedDropPercentage; }

    public Integer getVehicleCountBefore() { return vehicleCountBefore; }
    public void setVehicleCountBefore(Integer vehicleCountBefore) { this.vehicleCountBefore = vehicleCountBefore; }

    public Integer getVehicleCountDuring() { return vehicleCountDuring; }
    public void setVehicleCountDuring(Integer vehicleCountDuring) { this.vehicleCountDuring = vehicleCountDuring; }

    public Double getOccupancyBefore() { return occupancyBefore; }
    public void setOccupancyBefore(Double occupancyBefore) { this.occupancyBefore = occupancyBefore; }

    public Double getOccupancyDuring() { return occupancyDuring; }
    public void setOccupancyDuring(Double occupancyDuring) { this.occupancyDuring = occupancyDuring; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public Double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(Double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
}
