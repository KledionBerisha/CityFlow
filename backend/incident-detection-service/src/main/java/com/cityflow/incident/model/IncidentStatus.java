package com.cityflow.incident.model;

public enum IncidentStatus {
    DETECTED,     // Initially detected, not confirmed
    CONFIRMED,    // Confirmed by multiple sources or manual verification
    IN_PROGRESS,  // Response/resolution in progress
    RESOLVED,     // Incident resolved
    DISMISSED     // False positive, dismissed
}
