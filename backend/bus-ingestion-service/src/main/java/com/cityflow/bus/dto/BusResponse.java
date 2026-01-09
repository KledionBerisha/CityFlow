package com.cityflow.bus.dto;

import java.time.Instant;
import java.util.UUID;

public class BusResponse {
    private String id;
    private String vehicleId;
    private String licensePlate;
    private UUID currentRouteId;
    private String status;
    private Integer capacity;
    private String model;
    private Instant createdAt;
    private Instant updatedAt;

    public BusResponse() {
    }

    public static BusResponseBuilder builder() {
        return new BusResponseBuilder();
    }

    public static class BusResponseBuilder {
        private String id;
        private String vehicleId;
        private String licensePlate;
        private UUID currentRouteId;
        private String status;
        private Integer capacity;
        private String model;
        private Instant createdAt;
        private Instant updatedAt;

        public BusResponseBuilder id(String id) { this.id = id; return this; }
        public BusResponseBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public BusResponseBuilder licensePlate(String licensePlate) { this.licensePlate = licensePlate; return this; }
        public BusResponseBuilder currentRouteId(UUID currentRouteId) { this.currentRouteId = currentRouteId; return this; }
        public BusResponseBuilder status(String status) { this.status = status; return this; }
        public BusResponseBuilder capacity(Integer capacity) { this.capacity = capacity; return this; }
        public BusResponseBuilder model(String model) { this.model = model; return this; }
        public BusResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public BusResponseBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public BusResponse build() {
            BusResponse response = new BusResponse();
            response.id = this.id;
            response.vehicleId = this.vehicleId;
            response.licensePlate = this.licensePlate;
            response.currentRouteId = this.currentRouteId;
            response.status = this.status;
            response.capacity = this.capacity;
            response.model = this.model;
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

    public UUID getCurrentRouteId() { return currentRouteId; }
    public void setCurrentRouteId(UUID currentRouteId) { this.currentRouteId = currentRouteId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
