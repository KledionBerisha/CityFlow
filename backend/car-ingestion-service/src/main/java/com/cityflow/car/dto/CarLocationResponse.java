package com.cityflow.car.dto;

import java.time.Instant;

public class CarLocationResponse {
    
    private String id;
    private String carId;
    private String vehicleId;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;
    private Instant timestamp;
    private String source;
    private Double trafficDensity;
    private Double congestionLevel;

    public CarLocationResponse() {
    }

    public static CarLocationResponseBuilder builder() {
        return new CarLocationResponseBuilder();
    }

    public static class CarLocationResponseBuilder {
        private String id;
        private String carId;
        private String vehicleId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Instant timestamp;
        private String source;
        private Double trafficDensity;
        private Double congestionLevel;

        public CarLocationResponseBuilder id(String id) { this.id = id; return this; }
        public CarLocationResponseBuilder carId(String carId) { this.carId = carId; return this; }
        public CarLocationResponseBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public CarLocationResponseBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public CarLocationResponseBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public CarLocationResponseBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public CarLocationResponseBuilder heading(Double heading) { this.heading = heading; return this; }
        public CarLocationResponseBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public CarLocationResponseBuilder source(String source) { this.source = source; return this; }
        public CarLocationResponseBuilder trafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; return this; }
        public CarLocationResponseBuilder congestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; return this; }

        public CarLocationResponse build() {
            CarLocationResponse response = new CarLocationResponse();
            response.id = this.id;
            response.carId = this.carId;
            response.vehicleId = this.vehicleId;
            response.latitude = this.latitude;
            response.longitude = this.longitude;
            response.speedKmh = this.speedKmh;
            response.heading = this.heading;
            response.timestamp = this.timestamp;
            response.source = this.source;
            response.trafficDensity = this.trafficDensity;
            response.congestionLevel = this.congestionLevel;
            return response;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Double getTrafficDensity() { return trafficDensity; }
    public void setTrafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; }

    public Double getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; }
}

