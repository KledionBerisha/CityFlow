package com.cityflow.car.event;

import java.time.Instant;

public class CarLocationEvent {

    private String eventId;
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

    public CarLocationEvent() {
    }

    public static CarLocationEventBuilder builder() {
        return new CarLocationEventBuilder();
    }

    public static class CarLocationEventBuilder {
        private String eventId;
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

        public CarLocationEventBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public CarLocationEventBuilder carId(String carId) { this.carId = carId; return this; }
        public CarLocationEventBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public CarLocationEventBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public CarLocationEventBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public CarLocationEventBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public CarLocationEventBuilder heading(Double heading) { this.heading = heading; return this; }
        public CarLocationEventBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public CarLocationEventBuilder source(String source) { this.source = source; return this; }
        public CarLocationEventBuilder trafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; return this; }
        public CarLocationEventBuilder congestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; return this; }

        public CarLocationEvent build() {
            CarLocationEvent event = new CarLocationEvent();
            event.eventId = this.eventId;
            event.carId = this.carId;
            event.vehicleId = this.vehicleId;
            event.latitude = this.latitude;
            event.longitude = this.longitude;
            event.speedKmh = this.speedKmh;
            event.heading = this.heading;
            event.timestamp = this.timestamp;
            event.source = this.source;
            event.trafficDensity = this.trafficDensity;
            event.congestionLevel = this.congestionLevel;
            return event;
        }
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

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

