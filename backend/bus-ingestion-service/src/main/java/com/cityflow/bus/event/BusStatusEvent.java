package com.cityflow.bus.event;

import java.time.Instant;
import java.util.UUID;

public class BusStatusEvent {

    private String eventId;
    private String busId;
    private String vehicleId;
    private String previousStatus;
    private String newStatus;
    private UUID routeId;
    private String reason;
    private Instant timestamp;

    public BusStatusEvent() {
    }

    public static BusStatusEventBuilder builder() {
        return new BusStatusEventBuilder();
    }

    public static class BusStatusEventBuilder {
        private String eventId;
        private String busId;
        private String vehicleId;
        private String previousStatus;
        private String newStatus;
        private UUID routeId;
        private String reason;
        private Instant timestamp;

        public BusStatusEventBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public BusStatusEventBuilder busId(String busId) { this.busId = busId; return this; }
        public BusStatusEventBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public BusStatusEventBuilder previousStatus(String previousStatus) { this.previousStatus = previousStatus; return this; }
        public BusStatusEventBuilder newStatus(String newStatus) { this.newStatus = newStatus; return this; }
        public BusStatusEventBuilder routeId(UUID routeId) { this.routeId = routeId; return this; }
        public BusStatusEventBuilder reason(String reason) { this.reason = reason; return this; }
        public BusStatusEventBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public BusStatusEvent build() {
            BusStatusEvent event = new BusStatusEvent();
            event.eventId = this.eventId;
            event.busId = this.busId;
            event.vehicleId = this.vehicleId;
            event.previousStatus = this.previousStatus;
            event.newStatus = this.newStatus;
            event.routeId = this.routeId;
            event.reason = this.reason;
            event.timestamp = this.timestamp;
            return event;
        }
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public UUID getRouteId() { return routeId; }
    public void setRouteId(UUID routeId) { this.routeId = routeId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
