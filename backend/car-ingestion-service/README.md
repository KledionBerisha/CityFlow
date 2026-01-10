# Car Ingestion Service

Real-time car tracking and traffic flow simulation service for CityFlow.

## Overview

This microservice handles:
- Car fleet management (CRUD operations)
- Real-time GPS location tracking
- Traffic flow simulation with congestion modeling
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
cd backend/car-ingestion-service
mvn spring-boot:run
```

The service will start on http://localhost:8083

## API Endpoints

### Car Management

**Create Car**
```bash
POST /cars
Content-Type: application/json

{
  "vehicleId": "CAR-001",
  "licensePlate": "AB-123",
  "status": "ACTIVE",
  "make": "Toyota",
  "model": "Sedan",
  "color": "Red"
}
```

**Get All Cars**
```bash
GET /cars
```

**Get Car by ID**
```bash
GET /cars/{id}
```

**Get Car by Vehicle ID**
```bash
GET /cars/vehicle/{vehicleId}
```

**Get Cars by Status**
```bash
GET /cars/status/{status}
```

Status values: `ACTIVE`, `PARKED`, `OFFLINE`

**Update Car Status**
```bash
PATCH /cars/{id}/status?status=PARKED
```

### Car Location Tracking

**Get All Current Locations**
```bash
GET /car-locations/current
```

**Get Current Location for Specific Car**
```bash
GET /car-locations/current/{carId}
```

**Get Location History**
```bash
GET /car-locations/history/{carId}?hours=24&limit=100
```

**Get Recent Location Updates** (for polling)
```bash
GET /car-locations/recent?seconds=30
```

**Get Locations in Geographic Area** (for traffic analysis)
```bash
GET /car-locations/area?minLat=42.0&maxLat=42.5&minLon=21.0&maxLon=21.5
```

**Stream Locations** (Server-Sent Events)
```bash
GET /car-locations/stream?interval=3
```

## Configuration

Key configuration properties (see `application.yml`):

```yaml
app:
  simulator:
    enabled: true              # Enable traffic simulator
    interval-ms: 3000          # Update frequency (3 seconds)
    base-speed-kmh: 50         # Base car speed
    max-speed-kmh: 80          # Maximum car speed
    initial-car-count: 100     # Number of cars to simulate
    city-bounds:               # Geographic bounds for simulation
      min-lat: 42.0
      max-lat: 42.5
      min-lon: 21.0
      max-lon: 21.5
    traffic-density-radius-km: 0.5  # Radius for traffic density calculation
  
  redis:
    ttl-seconds: 300           # Cache TTL (5 minutes)
  
  kafka:
    enabled: true
    topics:
      car-location: car.location.events
      car-status: car.status.events
```

## Traffic Flow Simulation

The service includes an advanced traffic flow simulator that:

### Key Features

- **Traffic Density Calculation**: Calculates the number of cars per km² in a 500-meter radius around each car
- **Congestion Modeling**: Cars slow down based on nearby traffic density and congestion levels
- **Dynamic Speed Adjustment**: Speed is automatically adjusted based on:
  - Number of nearby cars
  - Average speed of nearby traffic
  - Calculated congestion level (0.0 = free flow, 1.0 = gridlock)
- **Realistic Movement**: Cars move organically within city bounds, changing direction naturally
- **Automatic Car Generation**: Creates cars automatically to maintain the configured initial count

### How It Works

1. **Traffic Density**: For each car, the simulator calculates how many other cars are within a 500-meter radius
2. **Speed Calculation**: Based on density and congestion, each car's speed is adjusted:
   - Low density = higher speed (up to max speed)
   - High density = lower speed (down to minimum 5 km/h)
3. **Position Updates**: Cars move every 3 seconds (configurable) with speed adjusted for traffic conditions
4. **Boundary Handling**: Cars bounce back or change direction when reaching city boundaries

### Traffic Metrics

Each location update includes:
- **Traffic Density**: Cars per km² in the nearby area
- **Congestion Level**: 0.0 (free flow) to 1.0 (gridlock)

## Kafka Events

### Car Location Event
Topic: `car.location.events`

```json
{
  "eventId": "uuid",
  "carId": "car-id",
  "vehicleId": "CAR-001",
  "latitude": 42.123,
  "longitude": 21.456,
  "speedKmh": 45.5,
  "heading": 90.0,
  "timestamp": "2026-01-09T12:00:00Z",
  "source": "SIMULATOR",
  "trafficDensity": 25.5,
  "congestionLevel": 0.3
}
```

### Car Status Event
Topic: `car.status.events`

```json
{
  "eventId": "uuid",
  "carId": "car-id",
  "vehicleId": "CAR-001",
  "previousStatus": "ACTIVE",
  "newStatus": "PARKED",
  "timestamp": "2026-01-09T12:00:00Z"
}
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
- `car_read` - GET operations
- `car_write` - POST, PUT, PATCH, DELETE operations

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
docker build -t cityflow/car-ingestion-service:latest .
```

**Run with Docker Compose:**
```bash
docker-compose up car-ingestion-service
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

## Differences from Bus Service

While similar in structure to the bus ingestion service, the car service has key differences:

1. **No Route Following**: Cars move freely within city bounds, not along predefined routes
2. **Traffic Flow Logic**: Cars affect each other's speed based on nearby traffic density
3. **Higher Volume**: Designed to handle many more vehicles (100+ by default)
4. **Congestion Modeling**: Includes traffic density and congestion level metrics
5. **Organic Movement**: Cars change direction naturally, not following fixed paths
6. **Faster Updates**: 3-second intervals (vs 5 seconds for buses) due to higher vehicle count

## Future Enhancements

Potential improvements:
- [ ] Integration with real traffic data APIs
- [ ] Route planning and navigation for cars
- [ ] Traffic light and intersection simulation
- [ ] Multi-lane road simulation
- [ ] Accident and incident simulation
- [ ] Weather-based speed adjustments
- [ ] Time-of-day traffic patterns
- [ ] Integration with bus routes for shared road usage

