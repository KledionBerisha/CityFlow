package com.cityflow.bus.dto;

import java.time.Instant;
import java.util.UUID;

public class BusLocationResponse {
    private String id;
    private String busId;
    private String vehicleId;
    private UUID routeId;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;
    private Instant timestamp;
    private Integer occupancy;
    private String source;

    public BusLocationResponse() {
    }

    public static BusLocationResponseBuilder builder() {
        return new BusLocationResponseBuilder();
    }

    public static class BusLocationResponseBuilder {
        private String id;
        private String busId;
        private String vehicleId;
        private UUID routeId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Instant timestamp;
        private Integer occupancy;
        private String source;

        public BusLocationResponseBuilder id(String id) { this.id = id; return this; }
        public BusLocationResponseBuilder busId(String busId) { this.busId = busId; return this; }
        public BusLocationResponseBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public BusLocationResponseBuilder routeId(UUID routeId) { this.routeId = routeId; return this; }
        public BusLocationResponseBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public BusLocationResponseBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public BusLocationResponseBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public BusLocationResponseBuilder heading(Double heading) { this.heading = heading; return this; }
        public BusLocationResponseBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public BusLocationResponseBuilder occupancy(Integer occupancy) { this.occupancy = occupancy; return this; }
        public BusLocationResponseBuilder source(String source) { this.source = source; return this; }

        public BusLocationResponse build() {
            BusLocationResponse response = new BusLocationResponse();
            response.id = this.id;
            response.busId = this.busId;
            response.vehicleId = this.vehicleId;
            response.routeId = this.routeId;
            response.latitude = this.latitude;
            response.longitude = this.longitude;
            response.speedKmh = this.speedKmh;
            response.heading = this.heading;
            response.timestamp = this.timestamp;
            response.occupancy = this.occupancy;
            response.source = this.source;
            return response;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public Integer getOccupancy() { return occupancy; }
    public void setOccupancy(Integer occupancy) { this.occupancy = occupancy; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
