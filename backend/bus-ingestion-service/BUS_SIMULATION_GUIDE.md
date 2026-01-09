# Bus Movement Simulation Guide

## Overview

The bus ingestion service now includes an enhanced GPS simulator that moves buses along actual routes through the city. Buses follow predefined routes, stop at each bus stop, wait for passengers, and continue to the next stop.

## How It Works

### Route-Based Movement

1. **Route Fetching**: When a bus is assigned to a route, the simulator fetches the route's stops (with coordinates) from the Route Management Service.

2. **Stop-to-Stop Navigation**: Buses move from one stop to the next along the route, following the sequence order defined in the route.

3. **Stop Arrival**: When a bus reaches a stop (within 50 meters), it:
   - Stops moving (speed = 0)
   - Waits for a configured duration (default: 30 seconds)
   - Updates location with next stop information

4. **Stop Departure**: After the wait time, the bus:
   - Resumes movement towards the next stop
   - Calculates heading based on the direction to the next stop

5. **Route Completion**: When a bus reaches the end of a route, it automatically loops back to the first stop and continues.

### Key Features

- ✅ **Realistic Movement**: Buses follow actual route geometry using stop coordinates
- ✅ **Stop Simulation**: Buses wait at stops for realistic passenger boarding
- ✅ **Heading Calculation**: Accurate heading based on direction to next stop
- ✅ **Distance Calculation**: Uses Haversine formula for accurate distance calculations
- ✅ **Next Stop Info**: Location updates include next stop ID, distance, and ETA
- ✅ **Route Caching**: Route stops are cached to reduce API calls
- ✅ **Fallback Mode**: If route data is unavailable, falls back to simple movement

## Configuration

Edit `application.yml` to configure the simulator:

```yaml
app:
  simulator:
    enabled: true              # Enable/disable simulator
    interval-ms: 5000          # Update frequency (5 seconds)
    speed-kmh: 40              # Average bus speed
    stop-wait-seconds: 30      # Time to wait at each stop
  route-service:
    url: http://localhost:8081 # Route Management Service URL
```

## Setting Up Routes

Before buses can move along routes, you need to:

1. **Create Stops** (via Route Management Service):
   ```bash
   POST http://localhost:8081/stops
   {
     "name": "Central Station",
     "latitude": 42.123456,
     "longitude": 21.123456,
     "terminal": true
   }
   ```

2. **Create Routes**:
   ```bash
   POST http://localhost:8081/routes
   {
     "code": "R1",
     "name": "Route 1 - Downtown Loop"
   }
   ```

3. **Assign Stops to Routes** (in sequence order):
   ```bash
   PUT http://localhost:8081/routes/{routeId}/stops
   [
     {
       "stopId": "stop-uuid-1",
       "sequenceOrder": 1
     },
     {
       "stopId": "stop-uuid-2",
       "sequenceOrder": 2
     }
   ]
   ```

4. **Assign Buses to Routes**:
   ```bash
   POST http://localhost:8082/buses
   {
     "vehicleId": "BUS-001",
     "currentRouteId": "route-uuid",
     "status": "ACTIVE"
   }
   ```

## How Buses Move

### Initialization

When a bus is first simulated:
- It starts at the first stop of its assigned route
- Heading is calculated towards the second stop

### Movement Between Stops

- Buses move at the configured speed (default: 40 km/h)
- Position is updated every interval (default: 5 seconds)
- Heading is continuously recalculated to point towards the next stop
- Distance to next stop is calculated and included in location updates

### Stop Behavior

- **Arrival**: Bus stops when within 50 meters of a stop
- **Waiting**: Bus remains stationary for `stop-wait-seconds`
- **Departure**: Bus resumes movement towards next stop
- **Location Updates**: Include next stop ID, distance, and estimated arrival time

### Route Looping

When a bus completes a route (reaches the last stop):
- It automatically loops back to the first stop
- Continues the route indefinitely

## Location Data

Each location update includes:

```json
{
  "busId": "bus-id",
  "vehicleId": "BUS-001",
  "routeId": "route-uuid",
  "latitude": 42.123456,
  "longitude": 21.123456,
  "speedKmh": 35.5,
  "heading": 90.0,
  "nextStopId": "stop-uuid",
  "distanceToNextStopKm": 0.5,
  "estimatedArrivalSeconds": 45,
  "timestamp": "2026-01-09T12:00:00Z"
}
```

## Monitoring

### View Current Bus Locations

```bash
# All buses
GET http://localhost:8082/bus-locations/current

# Specific bus
GET http://localhost:8082/bus-locations/current/{busId}

# Buses on a route
GET http://localhost:8082/bus-locations/current/route/{routeId}
```

### View Location History

```bash
GET http://localhost:8082/bus-locations/history/{busId}?hours=24&limit=100
```

## Troubleshooting

### Buses Not Moving

1. Check that buses have `status: ACTIVE`
2. Verify buses have a `currentRouteId` assigned
3. Ensure the route has stops defined
4. Check route service is accessible at configured URL

### Buses Moving Randomly

- This indicates fallback mode is active
- Check route service connectivity
- Verify route has stops with valid coordinates

### Performance

- Route stops are cached after first fetch
- If routes change, restart the service to clear cache
- Consider increasing `interval-ms` for slower updates

## Future Enhancements

Potential improvements:
- [ ] Support for route reversal (return trip)
- [ ] Traffic-aware speed adjustments
- [ ] Real-time route geometry (not just stops)
- [ ] Multiple buses on same route with spacing
- [ ] Schedule-based departure times
- [ ] Geofencing for automatic stop detection

