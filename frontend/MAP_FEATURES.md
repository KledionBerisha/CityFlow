# Map Features Implementation

This document describes the real-time map features that have been implemented.

## Features

### 1. Bus Markers 🚌
- **Location**: Blue bus icons displayed on the map
- **Real-time Updates**: Updates every 5 seconds
- **Features**:
  - Custom blue bus icon that rotates based on heading/direction
  - Popup shows: Bus ID, Speed, Occupancy, Next Stop
  - Uses data from `/api/bus-locations/current`

### 2. Traffic Sensor Markers 🚦
- **Location**: Colored circles at traffic sensor locations
- **Real-time Updates**: Updates every 5 seconds
- **Color Coding**:
  - 🔴 **Red** - SEVERE congestion
  - 🟠 **Orange** - HIGH congestion
  - 🟡 **Yellow** - MODERATE congestion
  - 🟢 **Green** - LOW congestion
- **Popup**: Shows Sensor ID, Speed, Vehicle Count, Congestion Level
- **Data Source**: `/api/traffic/readings/current`

### 3. Traffic-Based Road Highlighting 🛣️
- **Dynamic Colors**: Roads change color based on traffic conditions
- **Update Frequency**: Every 10 seconds
- **Color Scheme**:
  - 🔴 **Red** (thick, weight: 6) - SEVERE traffic
  - 🟠 **Orange** (weight: 5) - HIGH traffic
  - 🟡 **Yellow** (weight: 4) - MODERATE traffic
  - 🟢 **Green** (weight: 3) - LOW traffic
  - ⚪ **Gray** (weight: 3) - Unknown/No data
- **Data Source**: `/api/traffic/roads` (needs to be implemented in backend)

## Components Created

### Hooks
- `useBusLocations.ts` - Fetches and manages bus location data
- `useTrafficReadings.ts` - Fetches and manages traffic sensor data

### Components
- `BusMarker.tsx` - Renders individual bus markers on the map
- `TrafficMarker.tsx` - Renders traffic sensor markers with color coding

### Updated Components
- `LiveMap.tsx` - Integrated all markers and road overlays
- `api.ts` - Added `RoadTrafficData` interface and `getRoadTrafficData()` function

## Backend Requirements

### Already Available
- ✅ `GET /api/bus-locations/current` - Returns array of bus locations
- ✅ `GET /api/traffic/readings/current` - Returns array of traffic readings

### Needs Implementation
- ⚠️ `GET /api/traffic/roads` - Should return road segments with traffic data

**Expected Response Format:**
```json
[
  {
    "roadId": "road-001",
    "roadName": "Main Street",
    "geometry": {
      "type": "LineString",
      "coordinates": [[21.1655, 42.6629], [21.1660, 42.6635]]
    },
    "congestionLevel": "HIGH",
    "averageSpeed": 25.5,
    "vehicleCount": 45
  }
]
```

## Usage

The map automatically:
1. Fetches bus locations every 5 seconds
2. Fetches traffic readings every 5 seconds
3. Fetches road traffic data every 10 seconds
4. Updates all markers and road colors in real-time

All markers are clickable and show detailed information in popups.

## Future Enhancements

- [ ] Add animation for bus movement between updates
- [ ] Add clustering for markers when zoomed out
- [ ] Add filters to show/hide buses or traffic sensors
- [ ] Add legend explaining color codes
- [ ] Add route lines for buses
- [ ] Add heatmap overlay for traffic density

