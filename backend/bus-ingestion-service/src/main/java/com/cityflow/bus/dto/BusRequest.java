package com.cityflow.bus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class BusRequest {
    
    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;
    
    private String licensePlate;
    
    private UUID currentRouteId;
    
    @NotNull(message = "Status is required")
    private String status;
    
    private Integer capacity;
    
    private String model;

    public BusRequest() {
    }

    public BusRequest(String vehicleId, String licensePlate, UUID currentRouteId, String status, Integer capacity, String model) {
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.currentRouteId = currentRouteId;
        this.status = status;
        this.capacity = capacity;
        this.model = model;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public UUID getCurrentRouteId() { return currentRouteId; }
    public void setCurrentRouteId(UUID currentRouteId) { this.currentRouteId = currentRouteId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
