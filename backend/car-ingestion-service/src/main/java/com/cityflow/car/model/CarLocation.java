package com.cityflow.car.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "car_locations")
@CompoundIndex(name = "car_timestamp_idx", def = "{'carId': 1, 'timestamp': -1}")
public class CarLocation {

    @Id
    private String id;

    @Indexed
    private String carId;

    private String vehicleId;

    private Double latitude;

    private Double longitude;

    private Double speedKmh;

    private Double heading;

    @Indexed
    private Instant timestamp;

    private LocationSource source;

    // Traffic flow metrics
    private Double trafficDensity;  // Cars per km² in nearby area
    private Double congestionLevel; // 0.0 (free) to 1.0 (gridlock)
    
    // Road information
    private String roadId;
    private String roadName;

    public enum LocationSource {
        GPS,
        SIMULATOR,
        MANUAL
    }

    // Constructors
    public CarLocation() {
    }

    public CarLocation(String id, String carId, String vehicleId, Double latitude, Double longitude,
                       Double speedKmh, Double heading, Instant timestamp, LocationSource source,
                       Double trafficDensity, Double congestionLevel, String roadId, String roadName) {
        this.id = id;
        this.carId = carId;
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = speedKmh;
        this.heading = heading;
        this.timestamp = timestamp;
        this.source = source;
        this.trafficDensity = trafficDensity;
        this.congestionLevel = congestionLevel;
        this.roadId = roadId;
        this.roadName = roadName;
    }

    // Builder
    public static CarLocationBuilder builder() {
        return new CarLocationBuilder();
    }

    public static class CarLocationBuilder {
        private String id;
        private String carId;
        private String vehicleId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Instant timestamp;
        private LocationSource source;
        private Double trafficDensity;
        private Double congestionLevel;
        private String roadId;
        private String roadName;

        public CarLocationBuilder id(String id) { this.id = id; return this; }
        public CarLocationBuilder carId(String carId) { this.carId = carId; return this; }
        public CarLocationBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public CarLocationBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public CarLocationBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public CarLocationBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public CarLocationBuilder heading(Double heading) { this.heading = heading; return this; }
        public CarLocationBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public CarLocationBuilder source(LocationSource source) { this.source = source; return this; }
        public CarLocationBuilder trafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; return this; }
        public CarLocationBuilder congestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; return this; }
        public CarLocationBuilder roadId(String roadId) { this.roadId = roadId; return this; }
        public CarLocationBuilder roadName(String roadName) { this.roadName = roadName; return this; }

        public CarLocation build() {
            return new CarLocation(id, carId, vehicleId, latitude, longitude, speedKmh, heading,
                    timestamp, source, trafficDensity, congestionLevel, roadId, roadName);
        }
    }

    // Getters and Setters
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

    public LocationSource getSource() { return source; }
    public void setSource(LocationSource source) { this.source = source; }

    public Double getTrafficDensity() { return trafficDensity; }
    public void setTrafficDensity(Double trafficDensity) { this.trafficDensity = trafficDensity; }

    public Double getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(Double congestionLevel) { this.congestionLevel = congestionLevel; }

    public String getRoadId() { return roadId; }
    public void setRoadId(String roadId) { this.roadId = roadId; }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
}

