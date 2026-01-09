package com.cityflow.traffic.model;

public enum SensorStatus {
    ACTIVE,         // Sensor is operational
    INACTIVE,       // Sensor is disabled
    MAINTENANCE,    // Under maintenance
    ERROR,          // Sensor malfunction
    OFFLINE         // Not connected
}
