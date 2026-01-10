package com.cityflow.traffic.dto;

import com.cityflow.traffic.model.CongestionLevel;

import java.util.List;

/**
 * DTO for road traffic data with GeoJSON-compatible structure
 * Used for displaying traffic overlays on maps
 */
public class RoadTrafficDto {

    private String roadId;
    private String roadName;
    private GeoJsonGeometry geometry;
    private String congestionLevel;  // LOW, MODERATE, HIGH, SEVERE
    private Double averageSpeed;
    private Integer vehicleCount;
    private String lastUpdated;

    public RoadTrafficDto() {}

    public RoadTrafficDto(String roadId, String roadName) {
        this.roadId = roadId;
        this.roadName = roadName;
    }

    // Getters and Setters
    public String getRoadId() {
        return roadId;
    }

    public void setRoadId(String roadId) {
        this.roadId = roadId;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public GeoJsonGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonGeometry geometry) {
        this.geometry = geometry;
    }

    public String getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(String congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public Double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public Integer getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(Integer vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * GeoJSON Geometry structure for LineString
     */
    public static class GeoJsonGeometry {
        private String type = "LineString";
        private List<List<Double>> coordinates;  // Array of [longitude, latitude] pairs

        public GeoJsonGeometry() {}

        public GeoJsonGeometry(List<List<Double>> coordinates) {
            this.coordinates = coordinates;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(List<List<Double>> coordinates) {
            this.coordinates = coordinates;
        }
    }

    /**
     * Helper method to map CongestionLevel enum to string
     */
    public static String mapCongestionLevel(CongestionLevel level) {
        if (level == null) {
            return "LOW";
        }
        switch (level) {
            case FREE_FLOW:
                return "LOW";
            case LIGHT:
                return "MODERATE";
            case MODERATE:
                return "MODERATE";
            case HEAVY:
                return "HIGH";
            case SEVERE:
                return "SEVERE";
            default:
                return "LOW";
        }
    }
}
