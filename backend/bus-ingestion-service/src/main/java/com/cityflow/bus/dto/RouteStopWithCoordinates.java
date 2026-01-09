package com.cityflow.bus.dto;

import java.util.UUID;

/**
 * DTO representing a route stop with coordinates for bus simulation
 */
public class RouteStopWithCoordinates {
    
    private UUID stopId;
    private int sequenceOrder;
    private String stopName;
    private String stopCode;
    private boolean terminal;
    private double latitude;
    private double longitude;
    
    public RouteStopWithCoordinates() {
    }
    
    public RouteStopWithCoordinates(UUID stopId, int sequenceOrder, String stopName, 
                                   String stopCode, boolean terminal, 
                                   double latitude, double longitude) {
        this.stopId = stopId;
        this.sequenceOrder = sequenceOrder;
        this.stopName = stopName;
        this.stopCode = stopCode;
        this.terminal = terminal;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public UUID getStopId() {
        return stopId;
    }
    
    public void setStopId(UUID stopId) {
        this.stopId = stopId;
    }
    
    public int getSequenceOrder() {
        return sequenceOrder;
    }
    
    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }
    
    public String getStopName() {
        return stopName;
    }
    
    public void setStopName(String stopName) {
        this.stopName = stopName;
    }
    
    public String getStopCode() {
        return stopCode;
    }
    
    public void setStopCode(String stopCode) {
        this.stopCode = stopCode;
    }
    
    public boolean isTerminal() {
        return terminal;
    }
    
    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}

