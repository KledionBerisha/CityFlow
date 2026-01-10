package com.cityflow.car.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CarRequest {
    
    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;
    
    private String licensePlate;
    
    @NotNull(message = "Status is required")
    private String status;
    
    private String make;
    
    private String model;
    
    private String color;

    public CarRequest() {
    }

    public CarRequest(String vehicleId, String licensePlate, String status, String make, String model, String color) {
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.status = status;
        this.make = make;
        this.model = model;
        this.color = color;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

