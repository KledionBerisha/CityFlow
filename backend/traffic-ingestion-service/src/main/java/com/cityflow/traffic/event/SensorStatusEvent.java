package com.cityflow.traffic.event;

import com.cityflow.traffic.model.SensorStatus;

import java.time.Instant;

public class SensorStatusEvent {

    private String eventId;
    private String eventType = "SENSOR_STATUS_CHANGE";
    private Instant timestamp;

    private String sensorId;
    private String sensorCode;
    private SensorStatus oldStatus;
    private SensorStatus newStatus;
    private String reason;

    public SensorStatusEvent() {
        this.timestamp = Instant.now();
    }

    public SensorStatusEvent(String sensorId, String sensorCode, SensorStatus oldStatus, SensorStatus newStatus) {
        this();
        this.sensorId = sensorId;
        this.sensorCode = sensorCode;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public void setSensorCode(String sensorCode) {
        this.sensorCode = sensorCode;
    }

    public SensorStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(SensorStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public SensorStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(SensorStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
