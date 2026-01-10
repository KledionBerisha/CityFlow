package com.cityflow.car.model;

import java.time.Instant;

/**
 * Lightweight model for Redis caching of current car positions
 */
public class CarLocationCache {

    private String carId;
    private String vehicleId;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;
    private Instant timestamp;
    private String status;
    private Double trafficDensity;
    private Double congestionLevel;

    // Constructors
    public CarLocationCache() {
    }

    public CarLocationCache(String carId, String vehicleId, Double latitude, Double longitude,
                            Double speedKmh, Double heading, Instant timestamp, String status,
                            Double trafficDensity, Double congestionLevel) {
        this.carId = carId;
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = speedKmh;
        this.heading = heading;
        this.timestamp = timestamp;
        this.status = status;
        this.trafficDensity = trafficDensity;
        this.congestionLevel = congestionLevel;
    }

    // Builder
    public static CarLocationCacheBuilder builder() {
        return new CarLocationCacheBuilder();
    }

    public static class CarLocationCacheBuilder {
        private String carId;
        private String vehicleId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Instant timestamp;
        private String status;
        private Double trafficDensity;
        private Double congestionLevel;

        public CarLocationCacheBuilder carId(String carId) { this.carId = carId; return this; }
        public CarLocationCacheBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public CarLocationCacheBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public CarLocationCacheBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public CarLocationCacheBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public CarLocationCacheBuilder heading(Double heading) { this.heading = heading; return this; }
        public CarLocationCacheBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public CarLocationCacheBuilder status(String status) { this.status = status; return this; }
        public CarLocationCacheBuilder trafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; return this; }
        public CarLocationCacheBuilder congestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; return this; }

        public CarLocationCache build() {
            return new CarLocationCache(carId, vehicleId, latitude, longitude, speedKmh, heading, timestamp, status,
                    trafficDensity, congestionLevel);
        }
    }

    // Getters and Setters
    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }

    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTrafficDensity() { return trafficDensity; }
    public void setTrafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; }

    public Double getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; }
}

