package com.cityflow.traffic.dto;

import com.cityflow.traffic.model.Sensor;
import com.cityflow.traffic.model.SensorStatus;
import com.cityflow.traffic.model.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class SensorDto {

    private String id;

    @NotBlank(message = "Sensor code is required")
    private String code;

    @NotBlank(message = "Sensor name is required")
    private String name;

    @NotNull(message = "Location is required")
    private LocationDto location;

    @NotNull(message = "Sensor type is required")
    private SensorType type;

    private SensorStatus status;
    private String roadSegmentId;
    private Integer laneCount;
    private Double speedLimit;
    private Instant createdAt;
    private Instant updatedAt;

    public SensorDto() {}

    public static SensorDto fromEntity(Sensor sensor) {
        SensorDto dto = new SensorDto();
        dto.setId(sensor.getId());
        dto.setCode(sensor.getCode());
        dto.setName(sensor.getName());
        if (sensor.getLocation() != null) {
            dto.setLocation(LocationDto.fromEntity(sensor.getLocation()));
        }
        dto.setType(sensor.getType());
        dto.setStatus(sensor.getStatus());
        dto.setRoadSegmentId(sensor.getRoadSegmentId());
        dto.setLaneCount(sensor.getLaneCount());
        dto.setSpeedLimit(sensor.getSpeedLimit());
        dto.setCreatedAt(sensor.getCreatedAt());
        dto.setUpdatedAt(sensor.getUpdatedAt());
        return dto;
    }

    public Sensor toEntity() {
        Sensor sensor = new Sensor();
        sensor.setId(this.id);
        sensor.setCode(this.code);
        sensor.setName(this.name);
        if (this.location != null) {
            sensor.setLocation(this.location.toEntity());
        }
        sensor.setType(this.type);
        if (this.status != null) {
            sensor.setStatus(this.status);
        }
        sensor.setRoadSegmentId(this.roadSegmentId);
        sensor.setLaneCount(this.laneCount);
        sensor.setSpeedLimit(this.speedLimit);
        return sensor;
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

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
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

    public static class LocationDto {
        private Double latitude;
        private Double longitude;
        private String address;

        public LocationDto() {}

        public static LocationDto fromEntity(Sensor.Location location) {
            LocationDto dto = new LocationDto();
            dto.setLatitude(location.getLatitude());
            dto.setLongitude(location.getLongitude());
            dto.setAddress(location.getAddress());
            return dto;
        }

        public Sensor.Location toEntity() {
            Sensor.Location location = new Sensor.Location();
            location.setLatitude(this.latitude);
            location.setLongitude(this.longitude);
            location.setAddress(this.address);
            return location;
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
