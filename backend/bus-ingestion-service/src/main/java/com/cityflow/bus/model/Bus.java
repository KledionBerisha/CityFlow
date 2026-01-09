package com.cityflow.bus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "buses")
public class Bus {

    @Id
    private String id;

    @Indexed(unique = true)
    private String vehicleId;  // Physical bus identifier (e.g., "BUS-001")

    private String licensePlate;

    private UUID currentRouteId;  // Currently assigned route

    private BusStatus status;

    private Integer capacity;

    private String model;

    private Instant createdAt;

    private Instant updatedAt;

    public enum BusStatus {
        ACTIVE,      // In service
        IDLE,        // Parked/waiting
        MAINTENANCE, // Under maintenance
        OFFLINE      // Not operational
    }

    // Constructors
    public Bus() {
    }

    public Bus(String id, String vehicleId, String licensePlate, UUID currentRouteId, 
               BusStatus status, Integer capacity, String model, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.currentRouteId = currentRouteId;
        this.status = status;
        this.capacity = capacity;
        this.model = model;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static BusBuilder builder() {
        return new BusBuilder();
    }

    public static class BusBuilder {
        private String id;
        private String vehicleId;
        private String licensePlate;
        private UUID currentRouteId;
        private BusStatus status;
        private Integer capacity;
        private String model;
        private Instant createdAt;
        private Instant updatedAt;

        public BusBuilder id(String id) {
            this.id = id;
            return this;
        }

        public BusBuilder vehicleId(String vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public BusBuilder licensePlate(String licensePlate) {
            this.licensePlate = licensePlate;
            return this;
        }

        public BusBuilder currentRouteId(UUID currentRouteId) {
            this.currentRouteId = currentRouteId;
            return this;
        }

        public BusBuilder status(BusStatus status) {
            this.status = status;
            return this;
        }

        public BusBuilder capacity(Integer capacity) {
            this.capacity = capacity;
            return this;
        }

        public BusBuilder model(String model) {
            this.model = model;
            return this;
        }

        public BusBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BusBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Bus build() {
            return new Bus(id, vehicleId, licensePlate, currentRouteId, status, capacity, model, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public UUID getCurrentRouteId() {
        return currentRouteId;
    }

    public void setCurrentRouteId(UUID currentRouteId) {
        this.currentRouteId = currentRouteId;
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
        this.status = status;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
