# Frontend Integration Guide

This guide explains how to integrate the bus tracking system with your frontend application.

## API Endpoints

All endpoints are available through the API Gateway at `http://localhost:8000/api/` or directly at `http://localhost:8082/`.

### REST Endpoints

#### Get All Current Bus Locations
```http
GET /api/bus-locations/current
```

Returns all active buses with their current positions.

**Response:**
```json
[
  {
    "busId": "bus-uuid",
    "vehicleId": "BUS-001",
    "routeId": "route-uuid",
    "latitude": 42.123456,
    "longitude": 21.123456,
    "speedKmh": 35.5,
    "heading": 90.0,
    "timestamp": "2026-01-09T12:00:00Z",
    "occupancy": 25,
    "source": "SIMULATOR",
    "nextStopId": "stop-uuid",
    "distanceToNextStopKm": 0.5,
    "estimatedArrivalSeconds": 45
  }
]
```

#### Get Current Location for Specific Bus
```http
GET /api/bus-locations/current/{busId}
```

#### Get Current Locations by Route
```http
GET /api/bus-locations/current/route/{routeId}
```

#### Get Location History
```http
GET /api/bus-locations/history/{busId}?hours=24&limit=100
```

### Real-Time Streaming (Server-Sent Events)

#### Stream All Bus Locations
```http
GET /api/bus-locations/stream
Accept: text/event-stream
```

#### Stream Locations for Specific Route
```http
GET /api/bus-locations/stream?routeId={routeId}
Accept: text/event-stream
```

#### Custom Update Interval
```http
GET /api/bus-locations/stream?interval=5
Accept: text/event-stream
```

Default interval is 5 seconds (matches simulator update frequency).

## Frontend Implementation Examples

### JavaScript/TypeScript with EventSource (SSE)

```javascript
// Connect to SSE stream
const eventSource = new EventSource('http://localhost:8000/api/bus-locations/stream');

// Listen for location updates
eventSource.addEventListener('location-update', (event) => {
  const location = JSON.parse(event.data);
  updateBusOnMap(location);
});

// Listen for errors
eventSource.addEventListener('error', (event) => {
  console.error('SSE connection error:', event);
  // Implement reconnection logic
});

// Close connection when done
// eventSource.close();
```

### React Example

```jsx
import { useEffect, useState } from 'react';

function BusTracker() {
  const [buses, setBuses] = useState(new Map());

  useEffect(() => {
    const eventSource = new EventSource('http://localhost:8000/api/bus-locations/stream');

    eventSource.addEventListener('location-update', (event) => {
      const location = JSON.parse(event.data);
      setBuses(prev => new Map(prev).set(location.busId, location));
    });

    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      eventSource.close();
      // Implement reconnection
    };

    return () => {
      eventSource.close();
    };
  }, []);

  return (
    <div>
      {Array.from(buses.values()).map(bus => (
        <div key={bus.busId}>
          <p>Bus {bus.vehicleId}: {bus.latitude}, {bus.longitude}</p>
          <p>Speed: {bus.speedKmh} km/h</p>
          <p>Next Stop ETA: {bus.estimatedArrivalSeconds}s</p>
        </div>
      ))}
    </div>
  );
}
```

### Fetch API (Polling Alternative)

If SSE is not available, use polling:

```javascript
async function pollBusLocations() {
  const response = await fetch('http://localhost:8000/api/bus-locations/current');
  const locations = await response.json();
  updateBusesOnMap(locations);
}

// Poll every 5 seconds
setInterval(pollBusLocations, 5000);
```

### Map Integration (Leaflet Example)

```javascript
import L from 'leaflet';

// Create map
const map = L.map('map').setView([42.0, 21.0], 13);

// Create bus markers
const busMarkers = new Map();

// Connect to SSE stream
const eventSource = new EventSource('http://localhost:8000/api/bus-locations/stream');

eventSource.addEventListener('location-update', (event) => {
  const location = JSON.parse(event.data);
  
  // Get or create marker
  let marker = busMarkers.get(location.busId);
  
  if (!marker) {
    // Create new marker with bus icon
    const busIcon = L.icon({
      iconUrl: '/bus-icon.png',
      iconSize: [32, 32],
      iconAnchor: [16, 16],
    });
    
    marker = L.marker([location.latitude, location.longitude], {
      icon: busIcon,
      rotationAngle: location.heading, // If using rotated icons
    }).addTo(map);
    
    busMarkers.set(location.busId, marker);
  } else {
    // Update existing marker position and rotation
    marker.setLatLng([location.latitude, location.longitude]);
    // Update rotation if supported by your icon library
  }
  
  // Update popup with bus info
  marker.bindPopup(`
    <b>Bus ${location.vehicleId}</b><br>
    Speed: ${location.speedKmh} km/h<br>
    Next Stop: ${location.distanceToNextStopKm} km<br>
    ETA: ${location.estimatedArrivalSeconds}s
  `);
});
```

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `busId` | String | Unique bus identifier |
| `vehicleId` | String | Human-readable bus ID (e.g., "BUS-001") |
| `routeId` | UUID | Current route assignment |
| `latitude` | Double | Current latitude |
| `longitude` | Double | Current longitude |
| `speedKmh` | Double | Current speed in km/h |
| `heading` | Double | Direction in degrees (0-360, 0 = North) |
| `timestamp` | Instant | Location timestamp (ISO 8601) |
| `occupancy` | Integer | Number of passengers |
| `source` | String | Data source ("SIMULATOR", "GPS", "MANUAL") |
| `nextStopId` | String | UUID of next stop (null if none) |
| `distanceToNextStopKm` | Double | Distance to next stop in kilometers |
| `estimatedArrivalSeconds` | Integer | Estimated time to next stop in seconds |

## CORS Configuration

The API Gateway is configured to allow CORS requests. If you encounter CORS issues:

1. Ensure requests go through the API Gateway (`http://localhost:8000`)
2. Include proper headers in requests
3. Check API Gateway CORS configuration

## Error Handling

### SSE Connection Errors

```javascript
eventSource.onerror = (error) => {
  console.error('SSE connection lost');
  // Implement exponential backoff reconnection
  setTimeout(() => {
    // Reconnect logic
  }, 5000);
};
```

### HTTP Error Responses

- `404`: Bus or route not found
- `503`: Service unavailable (route service down)
- `429`: Rate limit exceeded (via API Gateway)

## Performance Considerations

1. **SSE vs Polling**: SSE is more efficient for real-time updates
2. **Update Frequency**: Default 5 seconds matches simulator interval
3. **Caching**: Current locations are cached in Redis for fast access
4. **Rate Limiting**: API Gateway limits to 20 req/sec for location endpoints

## Testing

### Test SSE Stream
```bash
curl -N http://localhost:8000/api/bus-locations/stream
```

### Test REST Endpoint
```bash
curl http://localhost:8000/api/bus-locations/current
```

## Security

- OAuth2/JWT authentication is enabled by default
- For development, set `APP_SECURITY_ENABLED=false` in application.yml
- In production, ensure proper authentication tokens are included in requests

## Next Steps

1. Set up routes and stops via Route Management Service
2. Create buses and assign them to routes
3. Connect frontend to SSE stream or use polling
4. Display buses on map with real-time updates
5. Show next stop information and ETAs

For more details, see [BUS_SIMULATION_GUIDE.md](./BUS_SIMULATION_GUIDE.md).

