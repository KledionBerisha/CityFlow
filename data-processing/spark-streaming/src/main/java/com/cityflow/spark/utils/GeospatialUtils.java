package com.cityflow.spark.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * Geospatial utilities for distance calculation and coordinate validation
 */
public class GeospatialUtils implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(GeospatialUtils.class);
    private static final long serialVersionUID = 1L;
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double EARTH_RADIUS_M = 6371000.0;
    
    /**
     * Calculate Haversine distance between two points in meters
     */
    public static double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2) {
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_M * c;
    }
    
    /**
     * Calculate speed in km/h from two points and time difference
     */
    public static double calculateSpeed(
            double lat1, double lon1, long timestamp1,
            double lat2, double lon2, long timestamp2) {
        
        double distanceMeters = calculateDistance(lat1, lon1, lat2, lon2);
        double timeSeconds = (timestamp2 - timestamp1) / 1000.0;
        
        if (timeSeconds <= 0) {
            return 0.0;
        }
        
        // Convert m/s to km/h
        return (distanceMeters / timeSeconds) * 3.6;
    }
    
    /**
     * Calculate bearing (heading) between two points in degrees
     */
    public static double calculateBearing(
            double lat1, double lon1,
            double lat2, double lon2) {
        
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        
        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);
        
        double bearing = Math.toDegrees(Math.atan2(y, x));
        
        // Normalize to 0-360
        return (bearing + 360) % 360;
    }
    
    /**
     * Validate if coordinates are within valid ranges
     */
    public static boolean isValidCoordinate(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180;
    }
    
    /**
     * Check if point is within bounding box
     */
    public static boolean isWithinBoundingBox(
            double lat, double lon,
            double minLat, double maxLat,
            double minLon, double maxLon) {
        
        return lat >= minLat && lat <= maxLat &&
               lon >= minLon && lon <= maxLon;
    }
    
    /**
     * Round coordinates to specified decimal places
     */
    public static double roundCoordinate(double coordinate, int decimalPlaces) {
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.round(coordinate * multiplier) / multiplier;
    }
    
    /**
     * Convert meters to degrees (approximate, varies by latitude)
     */
    public static double metersToDegrees(double meters, double latitude) {
        // 1 degree latitude ≈ 111km
        double latDegrees = meters / 111000.0;
        
        // Longitude varies by latitude
        double lonDegrees = meters / (111000.0 * Math.cos(Math.toRadians(latitude)));
        
        return (latDegrees + lonDegrees) / 2;
    }
    
    /**
     * Check if two locations are within specified distance threshold
     */
    public static boolean isWithinDistance(
            double lat1, double lon1,
            double lat2, double lon2,
            double thresholdMeters) {
        
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= thresholdMeters;
    }
}
