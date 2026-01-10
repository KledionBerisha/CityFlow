package com.cityflow.car.event;

import java.time.Instant;

public class CarStatusEvent {

    private String eventId;
    private String carId;
    private String vehicleId;
    private String previousStatus;
    private String newStatus;
    private Instant timestamp;

    public CarStatusEvent() {
    }

    public static CarStatusEventBuilder builder() {
        return new CarStatusEventBuilder();
    }

    public static class CarStatusEventBuilder {
        private String eventId;
        private String carId;
        private String vehicleId;
        private String previousStatus;
        private String newStatus;
        private Instant timestamp;

        public CarStatusEventBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public CarStatusEventBuilder carId(String carId) { this.carId = carId; return this; }
        public CarStatusEventBuilder vehicleId(String vehicleId) { this.vehicleId = vehicleId; return this; }
        public CarStatusEventBuilder previousStatus(String previousStatus) { this.previousStatus = previousStatus; return this; }
        public CarStatusEventBuilder newStatus(String newStatus) { this.newStatus = newStatus; return this; }
        public CarStatusEventBuilder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public CarStatusEvent build() {
            CarStatusEvent event = new CarStatusEvent();
            event.eventId = this.eventId;
            event.carId = this.carId;
            event.vehicleId = this.vehicleId;
            event.previousStatus = this.previousStatus;
            event.newStatus = this.newStatus;
            event.timestamp = this.timestamp;
            return event;
        }
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

