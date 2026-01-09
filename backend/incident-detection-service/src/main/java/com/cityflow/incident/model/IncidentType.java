package com.cityflow.incident.model;

public enum IncidentType {
    ACCIDENT,              // Traffic accident detected
    SEVERE_CONGESTION,     // Unusual severe traffic congestion
    ROAD_CLOSURE,          // Road blocked/closed
    BUS_BREAKDOWN,         // Bus malfunction/offline
    SENSOR_MALFUNCTION,    // Traffic sensor offline/error
    WEATHER_RELATED,       // Weather-induced incident
    CONSTRUCTION,          // Road construction/maintenance
    SPECIAL_EVENT,         // Event causing traffic impact
    UNKNOWN                // Unclassified incident
}
