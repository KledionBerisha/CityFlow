package com.cityflow.bus.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight model for Redis caching of current bus positions
 */
public class BusLocationCache {

    private String busId;
    private String vehicleId;
    private UUID routeId;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;
    private Instant timestamp;
    private String status;

    // Constructors
    public BusLocationCache() {
    }

    public BusLocationCache(String busId, String vehicleId, UUID routeId, Double latitude, Double longitude,
                            Double speedKmh, Double heading, Instant timestamp, String status) {
        this.busId = busId;
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = speedKmh;
        this.heading = heading;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Builder
    public static BusLocationCacheBuilder builder() {
        return new BusLocationCacheBuilder();
    }

    public static class BusLocationCacheBuilder {
        private String busId;
        private String vehicleId;
        private UUID routeId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Instant timestamp;
        private String status;

        public BusLocationCacheBuilder busId(String busId) { this.busId = busId; return this; }
        public BusLocationCacheBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public BusLocationCacheBuilder routeId(UUID routeId) { this.routeId = routeId; return this; }
        public BusLocationCacheBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public BusLocationCacheBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public BusLocationCacheBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public BusLocationCacheBuilder heading(Double heading) { this.heading = heading; return this; }
        public BusLocationCacheBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public BusLocationCacheBuilder status(String status) { this.status = status; return this; }

        public BusLocationCache build() {
            return new BusLocationCache(busId, vehicleId, routeId, latitude, longitude, speedKmh, heading, timestamp, status);
        }
    }

    // Getters and Setters
    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public UUID getRouteId() { return routeId; }
    public void setRouteId(UUID routeId) { this.routeId = routeId; }

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
}
