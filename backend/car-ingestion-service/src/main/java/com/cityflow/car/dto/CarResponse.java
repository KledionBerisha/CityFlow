package com.cityflow.car.dto;

import java.time.Instant;

public class CarResponse {
    
    private String id;
    private String vehicleId;
    private String licensePlate;
    private String status;
    private String make;
    private String model;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;

    public CarResponse() {
    }

    public static CarResponseBuilder builder() {
        return new CarResponseBuilder();
    }

    public static class CarResponseBuilder {
        private String id;
        private String vehicleId;
        private String licensePlate;
        private String status;
        private String make;
        private String model;
        private String color;
        private Instant createdAt;
        private Instant updatedAt;

        public CarResponseBuilder id(String id) { this.id = id; return this; }
        public CarResponseBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public CarResponseBuilder licensePlate(String licensePlate) { this.licensePlate = licensePlate; return this; }
        public CarResponseBuilder status(String status) { this.status = status; return this; }
        public CarResponseBuilder make(String make) { this.make = make; return this; }
        public CarResponseBuilder model(String model) { this.model = model; return this; }
        public CarResponseBuilder color(String color) { this.color = color; return this; }
        public CarResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public CarResponseBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public CarResponse build() {
            CarResponse response = new CarResponse();
            response.id = this.id;
            response.vehicleId = this.vehicleId;
            response.licensePlate = this.licensePlate;
            response.status = this.status;
            response.make = this.make;
            response.model = this.model;
            response.color = this.color;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            return response;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

