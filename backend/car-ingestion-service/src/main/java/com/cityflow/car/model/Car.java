package com.cityflow.car.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "cars")
public class Car {

    @Id
    private String id;

    @Indexed(unique = true)
    private String vehicleId;  // Physical car identifier (e.g., "CAR-001")

    private String licensePlate;

    private CarStatus status;

    private String make;

    private String model;

    private String color;

    private Instant createdAt;

    private Instant updatedAt;

    public enum CarStatus {
        ACTIVE,      // In traffic
        PARKED,      // Parked/stopped
        OFFLINE      // Not operational
    }

    // Constructors
    public Car() {
    }

    public Car(String id, String vehicleId, String licensePlate, CarStatus status, 
               String make, String model, String color, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.status = status;
        this.make = make;
        this.model = model;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static CarBuilder builder() {
        return new CarBuilder();
    }

    public static class CarBuilder {
        private String id;
        private String vehicleId;
        private String licensePlate;
        private CarStatus status;
        private String make;
        private String model;
        private String color;
        private Instant createdAt;
        private Instant updatedAt;

        public CarBuilder id(String id) {
            this.id = id;
            return this;
        }

        public CarBuilder vehicleId(String vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public CarBuilder licensePlate(String licensePlate) {
            this.licensePlate = licensePlate;
            return this;
        }

        public CarBuilder status(CarStatus status) {
            this.status = status;
            return this;
        }

        public CarBuilder make(String make) {
            this.make = make;
            return this;
        }

        public CarBuilder model(String model) {
            this.model = model;
            return this;
        }

        public CarBuilder color(String color) {
            this.color = color;
            return this;
        }

        public CarBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CarBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Car build() {
            return new Car(id, vehicleId, licensePlate, status, make, model, color, createdAt, updatedAt);
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

    public CarStatus getStatus() {
        return status;
    }

    public void setStatus(CarStatus status) {
        this.status = status;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

