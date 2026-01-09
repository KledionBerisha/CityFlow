package com.cityflow.bus.event;

import java.time.Instant;
import java.util.UUID;

public class BusLocationEvent {

    private String eventId;
    private String busId;
    private String vehicleId;
    private UUID routeId;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;
    private Instant timestamp;
    private String source;
    private Integer occupancy;
    private String nextStopId;
    private Double distanceToNextStopKm;
    private Integer estimatedArrivalSeconds;

    public BusLocationEvent() {
    }

    public static BusLocationEventBuilder builder() {
        return new BusLocationEventBuilder();
    }

    public static class BusLocationEventBuilder {
        private String eventId;
        private String busId;
        private String vehicleId;
        private UUID routeId;
        private Double latitude;
        private Double longitude;
        private Double speedKmh;
        private Double heading;
        private Instant timestamp;
        private String source;
        private Integer occupancy;
        private String nextStopId;
        private Double distanceToNextStopKm;
        private Integer estimatedArrivalSeconds;

        public BusLocationEventBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public BusLocationEventBuilder busId(String busId) { this.busId = busId; return this; }
        public BusLocationEventBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public BusLocationEventBuilder routeId(UUID routeId) { this.routeId = routeId; return this; }
        public BusLocationEventBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public BusLocationEventBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public BusLocationEventBuilder speedKmh(Double speedKmh) { this.speedKmh = speedKmh; return this; }
        public BusLocationEventBuilder heading(Double heading) { this.heading = heading; return this; }
        public BusLocationEventBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public BusLocationEventBuilder source(String source) { this.source = source; return this; }
        public BusLocationEventBuilder occupancy(Integer occupancy) { this.occupancy = occupancy; return this; }
        public BusLocationEventBuilder nextStopId(String nextStopId) { this.nextStopId = nextStopId; return this; }
        public BusLocationEventBuilder distanceToNextStopKm(Double distanceToNextStopKm) { this.distanceToNextStopKm = distanceToNextStopKm; return this; }
        public BusLocationEventBuilder estimatedArrivalSeconds(Integer estimatedArrivalSeconds) { this.estimatedArrivalSeconds = estimatedArrivalSeconds; return this; }

        public BusLocationEvent build() {
            BusLocationEvent event = new BusLocationEvent();
            event.eventId = this.eventId;
            event.busId = this.busId;
            event.vehicleId = this.vehicleId;
            event.routeId = this.routeId;
            event.latitude = this.latitude;
            event.longitude = this.longitude;
            event.speedKmh = this.speedKmh;
            event.heading = this.heading;
            event.timestamp = this.timestamp;
            event.source = this.source;
            event.occupancy = this.occupancy;
            event.nextStopId = this.nextStopId;
            event.distanceToNextStopKm = this.distanceToNextStopKm;
            event.estimatedArrivalSeconds = this.estimatedArrivalSeconds;
            return event;
        }
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

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

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getOccupancy() { return occupancy; }
    public void setOccupancy(Integer occupancy) { this.occupancy = occupancy; }

    public String getNextStopId() { return nextStopId; }
    public void setNextStopId(String nextStopId) { this.nextStopId = nextStopId; }

    public Double getDistanceToNextStopKm() { return distanceToNextStopKm; }
    public void setDistanceToNextStopKm(Double distanceToNextStopKm) { this.distanceToNextStopKm = distanceToNextStopKm; }

    public Integer getEstimatedArrivalSeconds() { return estimatedArrivalSeconds; }
    public void setEstimatedArrivalSeconds(Integer estimatedArrivalSeconds) { this.estimatedArrivalSeconds = estimatedArrivalSeconds; }
}
