package com.cityflow.traffic.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "sensors")
public class Sensor {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String name;
    private Location location;
    private SensorType type;
    private SensorStatus status;
    private String roadSegmentId;
    private Integer laneCount;
    private Double speedLimit;
    private Instant createdAt;
    private Instant updatedAt;

    public Sensor() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = SensorStatus.ACTIVE;
    }

    public Sensor(String code, String name, Location location, SensorType type) {
        this();
        this.code = code;
        this.name = name;
        this.location = location;
        this.type = type;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public SensorType getType() {
        return type;
    }

    public void setType(SensorType type) {
        this.type = type;
    }

    public SensorStatus getStatus() {
        return status;
    }

    public void setStatus(SensorStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getRoadSegmentId() {
        return roadSegmentId;
    }

    public void setRoadSegmentId(String roadSegmentId) {
        this.roadSegmentId = roadSegmentId;
    }

    public Integer getLaneCount() {
        return laneCount;
    }

    public void setLaneCount(Integer laneCount) {
        this.laneCount = laneCount;
    }

    public Double getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(Double speedLimit) {
        this.speedLimit = speedLimit;
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

    public static class Location {
        private Double latitude;
        private Double longitude;
        private String address;

        public Location() {}

        public Location(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }
}
