# Backend Integration Guide

This document outlines exactly which API endpoints need to be implemented in the backend and how the frontend expects the data.

## API Base URL

All API calls are made to: `http://localhost:8000/api` (via Vite proxy in development)

## Required Endpoints

### 1. Vehicle Count Statistics
**Endpoint:** `GET /api/traffic/vehicle-count`

**Expected Response:**
```json
{
  "avg": 173,
  "max": 371,
  "min": 87
}
```

**Frontend Usage:** `src/services/api.ts` → `getVehicleCount()`

---

### 2. Accident/Incident Count (24 Hours)
**Endpoint:** `GET /api/incidents/count?hours=24`

**Expected Response:**
```json
{
  "count": 2
}
```

**Frontend Usage:** `src/services/api.ts` → `getAccidentCount()`

**Note:** The incidents endpoint `GET /api/incidents` already exists and is used in the Events page.

---

### 3. Current Location
**Endpoint:** `GET /api/location/current`

**Expected Response (Option 1 - Simple):**
```json
{
  "location": "Prishtina"
}
```

**Expected Response (Option 2 - With Details):**
```json
{
  "name": "Prishtina",
  "latitude": 42.6629,
  "longitude": 21.1655
}
```

**Frontend Usage:** `src/services/api.ts` → `getCurrentLocation()`

---

### 4. Road Overlays (GeoJSON)
**Endpoint:** `GET /api/map/roads`

**Expected Response:**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [21.1655, 42.6629],
          [21.1660, 42.6635]
        ]
      },
      "properties": {
        "name": "Main Street",
        "type": "highway"
      }
    }
  ]
}
```

**Or as an array of features:**
```json
[
  {
    "type": "Feature",
    "geometry": {
      "type": "LineString",
      "coordinates": [[21.1655, 42.6629], [21.1660, 42.6635]]
    },
    "properties": {
      "name": "Main Street"
    }
  }
]
```

**Frontend Usage:** `src/services/api.ts` → `getRoadOverlays()`

**Note:** Roads will be rendered with yellow outline (#FFD700) and gray fill on the map.

---

### 5. Traffic Readings
**Endpoint:** `GET /api/traffic/readings/current`

**Expected Response:**
```json
[
  {
    "sensorId": "SENSOR-001",
    "latitude": 42.6629,
    "longitude": 21.1655,
    "speedKmh": 45.5,
    "vehicleCount": 25,
    "congestionLevel": "MODERATE",
    "timestamp": "2026-01-09T22:26:41Z"
  }
]
```

**Frontend Usage:** `src/services/api.ts` → `getTrafficReadings()`

---

### 6. Traffic Predictions
**Endpoint:** `GET /api/analytics/predictions`

**Expected Response:**
```json
[
  {
    "location": {
      "lat": 42.6629,
      "lng": 21.1655
    },
    "predictedSpeed": 40.5,
    "predictedCongestion": "HIGH",
    "confidence": 85.5,
    "timeWindow": "10-30 minutes"
  }
]
```

**Frontend Usage:** `src/services/api.ts` → `getTrafficPredictions()`

---

## Already Available Endpoints

These endpoints are already implemented in the backend and working:

- ✅ `GET /api/bus-locations/current` - Get all current bus locations
- ✅ `GET /api/bus-locations/current/route/{routeId}` - Get bus locations by route
- ✅ `GET /api/routes` - Get all routes
- ✅ `GET /api/incidents` - Get all incidents

---

## Implementation Notes

### Error Handling
All API functions in `src/services/api.ts` have try-catch blocks and will fall back to placeholder data if the endpoint is not available. This allows the frontend to work during development even if endpoints aren't ready.

### Real-Time Updates
The frontend currently polls for updates every 5 seconds. Consider implementing:
- **Server-Sent Events (SSE)** for real-time updates
- **WebSockets** for bidirectional communication
- The bus service already has SSE support at `/api/bus-locations/stream`

### CORS Configuration
Ensure the API Gateway has CORS configured to allow requests from `http://localhost:3000` during development.

### Authentication
Currently, the frontend doesn't include authentication headers. When ready, add JWT tokens to requests:
```typescript
headers: {
  'Authorization': `Bearer ${token}`
}
```

---

## Testing

To test the integration:

1. Start the backend services
2. Start the frontend: `cd frontend && npm run dev`
3. Open browser console to see API calls
4. Check Network tab for failed requests
5. Replace placeholder implementations in `src/services/api.ts` with actual fetch calls

---

## File Locations

- **API Service Layer:** `frontend/src/services/api.ts`
- **Map Data Hook:** `frontend/src/hooks/useMapData.ts`
- **Road Overlays Hook:** `frontend/src/hooks/useRoadOverlays.ts`
- **Live Map Component:** `frontend/src/pages/LiveMap.tsx`

