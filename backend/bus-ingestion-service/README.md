# Bus Ingestion Service

Real-time bus tracking and GPS data ingestion service for CityFlow.

## Overview

This microservice handles:
- Bus fleet management (CRUD operations)
- Real-time GPS location tracking
- GPS simulation for development/testing
- Location caching in Redis for fast access
- Historical location storage in MongoDB
- Kafka event streaming for location updates

## Technology Stack

- **Spring Boot 3.2.2** with WebFlux (Reactive)
- **MongoDB** - Historical location storage
- **Redis** - Real-time location caching
- **Apache Kafka** - Event streaming
- **OAuth2/JWT** - Security (Keycloak)

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for dependencies)

### Start Infrastructure
```bash
# From project root
docker-compose up -d mongo redis kafka keycloak
```

### Run Service
```bash
cd backend/bus-ingestion-service
mvn spring-boot:run
```

The service will start on http://localhost:8082

## API Endpoints

### Bus Management

**Create Bus**
```bash
POST /buses
Content-Type: application/json

{
  "vehicleId": "BUS-001",
  "licensePlate": "XYZ-123",
  "currentRouteId": "uuid-here",
  "status": "ACTIVE",
  "capacity": 50,
  "model": "Mercedes Citaro"
}
```

**Get All Buses**
```bash
GET /buses
```

**Get Bus by ID**
```bash
GET /buses/{id}
```

**Get Bus by Vehicle ID**
```bash
GET /buses/vehicle/{vehicleId}
```

**Get Buses by Route**
```bash
GET /buses/route/{routeId}
```

**Update Bus Status**
```bash
PATCH /buses/{id}/status?status=IDLE
```

Status values: `ACTIVE`, `IDLE`, `MAINTENANCE`, `OFFLINE`

### Bus Location Tracking

**Get All Current Locations**
```bash
GET /bus-locations/current
```

**Get Current Location for Specific Bus**
```bash
GET /bus-locations/current/{busId}
```

**Get Current Locations by Route**
```bash
GET /bus-locations/current/route/{routeId}
```

**Get Location History**
```bash
GET /bus-locations/history/{busId}?hours=24&limit=100
```

**Get Recent Location Updates** (for polling)
```bash
GET /bus-locations/recent?seconds=30
```

## Configuration

Key configuration properties (see `application.yml`):

```yaml
app:
  simulator:
    enabled: true           # Enable GPS simulator
    interval-ms: 5000       # Update frequency (5 seconds)
    speed-kmh: 40          # Average simulation speed
  
  redis:
    ttl-seconds: 300       # Cache TTL (5 minutes)
  
  kafka:
    enabled: true
    topics:
      bus-location: bus.location.events
      bus-status: bus.status.events
```

## Kafka Events

### Bus Location Event
Topic: `bus.location.events`

```json
{
  "eventId": "uuid",
  "busId": "bus-id",
  "vehicleId": "BUS-001",
  "routeId": "route-uuid",
  "latitude": 42.123,
  "longitude": 21.456,
  "speedKmh": 35.5,
  "heading": 90.0,
  "timestamp": "2026-01-09T12:00:00Z",
  "source": "SIMULATOR",
  "occupancy": 25
}
```

### Bus Status Event
Topic: `bus.status.events`

```json
{
  "eventId": "uuid",
  "busId": "bus-id",
  "vehicleId": "BUS-001",
  "previousStatus": "ACTIVE",
  "newStatus": "IDLE",
  "routeId": "route-uuid",
  "reason": "End of shift",
  "timestamp": "2026-01-09T12:00:00Z"
}
```

## GPS Simulator

The service includes a built-in GPS simulator that:
- Generates realistic bus movements for all buses with status `ACTIVE`
- Updates positions every 5 seconds (configurable)
- Simulates speed variations and heading changes
- Stores locations in MongoDB and caches in Redis
- Publishes events to Kafka

To disable the simulator:
```yaml
app:
  simulator:
    enabled: false
```

## Data Flow

1. **GPS Data Ingestion** → Simulator or Real GPS
2. **Location Storage** → MongoDB (persistent)
3. **Location Caching** → Redis (fast access, TTL-based)
4. **Event Publishing** → Kafka (downstream consumers)
5. **API Queries** → Served from Redis cache or MongoDB

## Security

Protected by OAuth2/JWT (Keycloak):

**Required Roles:**
- `bus_read` - GET operations
- `bus_write` - POST, PUT, PATCH, DELETE operations

To disable security (dev/test only):
```yaml
app:
  security:
    enabled: false
```

## Monitoring

Health and metrics endpoints:
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Service info
- `GET /actuator/metrics` - Prometheus metrics

## Docker

**Build Image:**
```bash
docker build -t cityflow/bus-ingestion-service:latest .
```

**Run with Docker Compose:**
```bash
docker-compose up bus-ingestion-service
```

## Development

**Run tests:**
```bash
mvn test
```

**Build JAR:**
```bash
mvn clean package
```

## Future Enhancements

- [ ] WebSocket support for real-time location streaming
- [ ] Route geometry integration for accurate path simulation
- [ ] ETA calculation based on historical data
- [ ] Geofencing for stop detection
- [ ] Integration with traffic data for speed adjustments
- [ ] Support for real GPS hardware integration
