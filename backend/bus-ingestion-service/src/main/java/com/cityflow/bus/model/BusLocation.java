package com.cityflow.bus.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "bus_locations")
@CompoundIndex(name = "bus_timestamp_idx", def = "{'busId': 1, 'timestamp': -1}")
public class BusLocation {

    @Id
    private String id;

    @Indexed
    private String busId;

    private String vehicleId;

    private UUID routeId;

    private Double latitude;

    private Double longitude;

    private Double speedKmh;

    private Double heading;

    @Indexed
    private Instant timestamp;

    private Integer occupancy;

    private String nextStopId;

    private Double distanceToNextStopKm;

    private Integer estimatedArrivalSeconds;

    private LocationSource source;

    public enum LocationSource {
        GPS,
        SIMULATOR,
        MANUAL
    }

    // Constructors
    public BusLocation() {
    }

    public BusLocation(String id, String busId, String vehicleId, UUID routeId, Double latitude, Double longitude,
                       Double speedKmh, Double heading, Instant timestamp, Integer occupancy, String nextStopId,
                       Double distanceToNextStopKm, Integer estimatedArrivalSeconds, LocationSource source) {
        this.id = id;
        this.busId = busId;
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = speedKmh;
        this.heading = heading;
        this.timestamp = timestamp;
        this.occupancy = occupancy;
        this.nextStopId = nextStopId;
        this.distanceToNextStopKm = distanceToNextStopKm;
        this.estimatedArrivalSeconds = estimatedArrivalSeconds;
        this.source = source;
    }

    // Builder
    public static BusLocationBuilder builder() {
        return new BusLocationBuilder();
    }

    public static class BusLocationBuilder {
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
        private String nextStopId;
        private Double distanceToNextStopKm;
        private Integer estimatedArrivalSeconds;
        private LocationSource source;

        public BusLocationBuilder id(String id) { this.id = id; return this; }
        public BusLocationBuilder busId(String busId) { this.busId = busId; return this; }
        public BusLocationBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public BusLocationBuilder routeId(UUID routeId) { this.routeId = routeId; return this; }
        public BusLocationBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public BusLocationBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public BusLocationBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public BusLocationBuilder heading(Double heading) { this.heading = heading; return this; }
        public BusLocationBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public BusLocationBuilder occupancy(Integer occupancy) { this.occupancy = occupancy; return this; }
        public BusLocationBuilder nextStopId(String nextStopId) { this.nextStopId = nextStopId; return this; }
        public BusLocationBuilder distanceToNextStopKm(Double distanceToNextStopKm) { this.distanceToNextStopKm = distanceToNextStopKm; return this; }
        public BusLocationBuilder estimatedArrivalSeconds(Integer estimatedArrivalSeconds) { this.estimatedArrivalSeconds = estimatedArrivalSeconds; return this; }
        public BusLocationBuilder source(LocationSource source) { this.source = source; return this; }

        public BusLocation build() {
            return new BusLocation(id, busId, vehicleId, routeId, latitude, longitude, speedKmh, heading,
                    timestamp, occupancy, nextStopId, distanceToNextStopKm, estimatedArrivalSeconds, source);
        }
    }

    // Getters and Setters
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

    public String getNextStopId() { return nextStopId; }
    public void setNextStopId(String nextStopId) { this.nextStopId = nextStopId; }

    public Double getDistanceToNextStopKm() { return distanceToNextStopKm; }
    public void setDistanceToNextStopKm(Double distanceToNextStopKm) { this.distanceToNextStopKm = distanceToNextStopKm; }

    public Integer getEstimatedArrivalSeconds() { return estimatedArrivalSeconds; }
    public void setEstimatedArrivalSeconds(Integer estimatedArrivalSeconds) { this.estimatedArrivalSeconds = estimatedArrivalSeconds; }

    public LocationSource getSource() { return source; }
    public void setSource(LocationSource source) { this.source = source; }
}
