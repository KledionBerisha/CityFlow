# Traffic Ingestion Service 🚦

Real-time traffic monitoring service for CityFlow. Collects, processes, and streams traffic data from sensors deployed across the city.

## Overview

The Traffic Ingestion Service is a reactive microservice that:
- Manages traffic sensors (speed sensors, loop detectors, cameras, radar)
- Collects real-time traffic readings (speed, vehicle count, congestion)
- Simulates realistic traffic patterns for development/testing
- Caches current readings in Redis for fast access
- Stores historical data in MongoDB for analytics
- Publishes events to Kafka for downstream processing

## Technology Stack

- **Spring Boot 3.2.2** with WebFlux (reactive)
- **MongoDB** - Historical traffic data storage
- **Redis** - Real-time data caching
- **Kafka** - Event streaming
- **OAuth2/JWT** - Security via Keycloak
- **Java 17** - Runtime

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- MongoDB running on `localhost:27017`
- Redis running on `localhost:6379`
- Kafka running on `localhost:9093`
- Keycloak on `localhost:8080` (optional, can be disabled)

### Build & Run

```bash
cd backend/traffic-ingestion-service

# Build
mvn clean package

# Run locally
mvn spring-boot:run

# Run with Docker
docker build -t traffic-ingestion-service .
docker run -p 8083:8083 \
  -e SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/cityflow \
  -e SPRING_DATA_REDIS_HOST=redis \
  traffic-ingestion-service
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8083` | HTTP server port |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/cityflow` | MongoDB connection |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | Kafka brokers |
| `OAUTH2_ISSUER_URI` | `http://localhost:8080/realms/cityflow` | Keycloak issuer |
| `APP_SECURITY_ENABLED` | `true` | Enable/disable OAuth2 security |
| `APP_KAFKA_ENABLED` | `true` | Enable/disable Kafka publishing |
| `APP_SIMULATOR_ENABLED` | `true` | Enable/disable traffic simulator |
| `APP_SIMULATOR_INTERVAL_MS` | `10000` | Simulator update interval (ms) |

## API Endpoints

### Sensor Management

#### Create Sensor
```bash
POST /sensors
Content-Type: application/json

{
  "code": "SENSOR-001",
  "name": "Main Street North",
  "location": {
    "latitude": 42.3601,
    "longitude": -71.0589,
    "address": "Main Street, Sample City"
  },
  "type": "SPEED",
  "roadSegmentId": "ROAD-SEG-001",
  "laneCount": 4,
  "speedLimit": 60.0
}
```

**Sensor Types:**
- `SPEED` - Speed measurement sensor
- `COUNT` - Vehicle counter
- `CAMERA` - Video camera
- `LOOP_DETECTOR` - Inductive loop in road
- `RADAR` - Radar sensor
- `LIDAR` - LIDAR sensor

#### List All Sensors
```bash
GET /sensors
```

#### Get Sensor by ID
```bash
GET /sensors/{id}
```

#### Get Sensor by Code
```bash
GET /sensors/code/{code}
```

#### Get Sensors by Status
```bash
GET /sensors/status/ACTIVE
```

**Sensor Status Values:**
- `ACTIVE` - Operational
- `INACTIVE` - Disabled
- `MAINTENANCE` - Under maintenance
- `ERROR` - Malfunctioning
- `OFFLINE` - Not connected

#### Get Sensors by Road Segment
```bash
GET /sensors/road-segment/{roadSegmentId}
```

#### Update Sensor Status
```bash
PATCH /sensors/{id}/status
Content-Type: application/json

{
  "status": "MAINTENANCE"
}
```

#### Delete Sensor
```bash
DELETE /sensors/{id}
```

### Traffic Data Retrieval

#### Get All Current Traffic Readings
```bash
GET /traffic/current
```
Returns cached readings for all sensors (fast, Redis-backed).

#### Get Current Reading for Sensor
```bash
GET /traffic/current/{sensorId}
```

#### Get Historical Data
```bash
GET /traffic/history/{sensorId}?limit=100
```

Query Parameters:
- `limit` (optional, default: 100) - Max number of records

#### Get Readings by Time Range
```bash
GET /traffic/history/{sensorId}/range?start=2024-01-09T10:00:00Z&end=2024-01-09T12:00:00Z
```

Query Parameters:
- `start` (required) - Start timestamp (ISO-8601)
- `end` (required) - End timestamp (ISO-8601)

#### Get Traffic by Road Segment
```bash
GET /traffic/road-segment/{roadSegmentId}?limit=50
```

#### Get Recent Readings
```bash
GET /traffic/recent?minutes=5
```

Query Parameters:
- `minutes` (optional, default: 5) - Time window in minutes

#### Get Readings by Congestion Level
```bash
GET /traffic/congestion/HEAVY
```

**Congestion Levels:**
- `FREE_FLOW` - < 30% capacity, speed > 80% of limit
- `LIGHT` - 30-50% capacity, speed 60-80% of limit
- `MODERATE` - 50-70% capacity, speed 40-60% of limit
- `HEAVY` - 70-90% capacity, speed 20-40% of limit
- `SEVERE` - > 90% capacity, speed < 20% of limit

#### Get Incident Readings
```bash
GET /traffic/incidents
```
Returns readings where `incidentDetected` is true.

## Traffic Simulator

The built-in simulator generates realistic traffic patterns:

### Features
- **Time-of-day variation**: Light traffic at night, heavy during peak hours
- **Peak hours**: 7-9 AM and 5-7 PM have increased congestion
- **Realistic metrics**: Speed, count, occupancy, queue length
- **Environmental data**: Temperature, weather conditions
- **Incident detection**: Random incidents during severe congestion
- **Configurable**: Interval and parameters adjustable

### How It Works

1. Runs every 10 seconds (configurable via `APP_SIMULATOR_INTERVAL_MS`)
2. Queries all sensors with `ACTIVE` status
3. Generates readings based on:
   - Current time of day
   - Random variation
   - Configured speed limits
   - Road capacity
4. Calculates congestion level from speed and occupancy
5. Stores reading in MongoDB
6. Caches in Redis (5 min TTL)
7. Publishes event to Kafka

### Sample Reading

```json
{
  "id": "65a1234567890abcdef12345",
  "sensorId": "65a9876543210fedcba98765",
  "sensorCode": "SENSOR-001",
  "roadSegmentId": "ROAD-SEG-001",
  "timestamp": "2024-01-09T14:30:00Z",
  "averageSpeed": 35.5,
  "vehicleCount": 45,
  "occupancy": 0.62,
  "congestionLevel": "MODERATE",
  "queueLength": 8,
  "temperature": 22.3,
  "weatherCondition": "CLEAR",
  "incidentDetected": false,
  "createdAt": "2024-01-09T14:30:01Z"
}
```

## Kafka Events

### Traffic Reading Event

**Topic:** `traffic.reading.events` (5 partitions)

**Key:** `sensorId`

**Payload:**
```json
{
  "eventId": "uuid-v4",
  "eventType": "TRAFFIC_READING",
  "timestamp": "2024-01-09T14:30:00Z",
  "sensorId": "sensor-id",
  "sensorCode": "SENSOR-001",
  "roadSegmentId": "ROAD-SEG-001",
  "averageSpeed": 35.5,
  "vehicleCount": 45,
  "occupancy": 0.62,
  "congestionLevel": "MODERATE",
  "queueLength": 8,
  "incidentDetected": false
}
```

### Sensor Status Event

**Topic:** `sensor.status.events` (3 partitions)

**Key:** `sensorId`

**Payload:**
```json
{
  "eventId": "uuid-v4",
  "eventType": "SENSOR_STATUS_CHANGE",
  "timestamp": "2024-01-09T14:30:00Z",
  "sensorId": "sensor-id",
  "sensorCode": "SENSOR-001",
  "oldStatus": "ACTIVE",
  "newStatus": "MAINTENANCE",
  "reason": null
}
```

## Data Models

### Sensor

```java
{
  "id": "string",
  "code": "string",
  "name": "string",
  "location": {
    "latitude": "double",
    "longitude": "double",
    "address": "string"
  },
  "type": "SPEED|COUNT|CAMERA|LOOP_DETECTOR|RADAR|LIDAR",
  "status": "ACTIVE|INACTIVE|MAINTENANCE|ERROR|OFFLINE",
  "roadSegmentId": "string",
  "laneCount": "integer",
  "speedLimit": "double",
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

### Traffic Reading

```java
{
  "id": "string",
  "sensorId": "string",
  "sensorCode": "string",
  "roadSegmentId": "string",
  "timestamp": "instant",
  "averageSpeed": "double",        // km/h
  "vehicleCount": "integer",       // vehicles in time window
  "occupancy": "double",           // 0.0 - 1.0 (percentage)
  "congestionLevel": "FREE_FLOW|LIGHT|MODERATE|HEAVY|SEVERE",
  "queueLength": "integer",        // vehicles waiting
  "temperature": "double",         // celsius
  "weatherCondition": "string",    // CLEAR, RAIN, FOG, CLOUDY
  "incidentDetected": "boolean",
  "createdAt": "instant"
}
```

## MongoDB Collections

### sensors
- Indexed on: `code` (unique)
- Stores sensor metadata

### traffic_readings
- Compound indexes:
  - `sensorId` + `timestamp` (desc)
  - `roadSegmentId` + `timestamp` (desc)
- Stores historical readings
- Time-series optimized

## Redis Cache Keys

- **Pattern:** `traffic:current:{sensorId}`
- **TTL:** 5 minutes (300 seconds)
- **Value:** JSON-serialized TrafficReading
- **Purpose:** Fast access to current readings

## Security

### OAuth2/JWT

The service acts as an OAuth2 Resource Server using Keycloak.

**Required Roles:**
- `traffic_read` or `SCOPE_traffic_read` - Read access to sensors and traffic data
- `traffic_write` or `SCOPE_traffic_write` - Write access (create/update/delete sensors)

**Public Endpoints** (no auth):
- `/actuator/health`
- `/actuator/info`

### Development Mode

Disable security for local testing:

```bash
export APP_SECURITY_ENABLED=false
mvn spring-boot:run
```

Or in `application.yml`:
```yaml
app:
  security:
    enabled: false
```

## Health & Monitoring

### Actuator Endpoints

- `GET /actuator/health` - Service health status
- `GET /actuator/info` - Service information
- `GET /actuator/metrics` - Prometheus metrics

### Health Checks

The service reports health based on:
- MongoDB connectivity
- Redis connectivity
- Application status

Example:
```bash
curl http://localhost:8083/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "mongo": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

## Sample Data

On startup, the service automatically creates 8 sample sensors if the database is empty:

1. **SENSOR-001** - Main Street North (SPEED)
2. **SENSOR-002** - Main Street South (SPEED)
3. **SENSOR-003** - Highway 101 Entry (LOOP_DETECTOR)
4. **SENSOR-004** - Downtown Junction (CAMERA)
5. **SENSOR-005** - Industrial Zone (COUNT)
6. **SENSOR-006** - University Avenue (RADAR)
7. **SENSOR-007** - Airport Road (SPEED, INACTIVE)
8. **SENSOR-008** - Shopping District (CAMERA)

## Integration with Other Services

### Analytics Service (Future)
Subscribe to `traffic.reading.events` to:
- Calculate real-time congestion heatmaps
- Detect traffic incidents
- Generate alerts for threshold violations
- Aggregate data for dashboards

### ML Service (Future)
Use historical data from MongoDB to:
- Train traffic prediction models
- Forecast congestion 10-30 minutes ahead
- Identify patterns and anomalies

### Notification Service (Future)
Subscribe to incident events to:
- Send alerts to users
- Notify traffic management
- Trigger automated responses

## Development

### Running Tests

```bash
mvn test
```

### Building Docker Image

```bash
docker build -t cityflow/traffic-ingestion-service:latest .
```

### Code Structure

```
src/main/java/com/cityflow/traffic/
├── TrafficIngestionServiceApplication.java  # Main entry point
├── config/                                   # Configuration classes
│   ├── DataInitializer.java                 # Sample data creation
│   ├── KafkaConfig.java                     # Kafka producer config
│   ├── RedisConfig.java                     # Redis template config
│   └── SecurityConfig.java                  # OAuth2 security config
├── dto/                                      # Data Transfer Objects
│   ├── SensorDto.java
│   ├── TrafficReadingDto.java
│   └── UpdateSensorStatusRequest.java
├── event/                                    # Kafka event models
│   ├── SensorStatusEvent.java
│   └── TrafficReadingEvent.java
├── model/                                    # Domain models
│   ├── CongestionLevel.java
│   ├── Sensor.java
│   ├── SensorStatus.java
│   ├── SensorType.java
│   └── TrafficReading.java
├── repository/                               # MongoDB repositories
│   ├── SensorRepository.java
│   └── TrafficReadingRepository.java
├── service/                                  # Business logic
│   ├── KafkaProducerService.java
│   ├── SensorService.java
│   └── TrafficReadingService.java
├── simulator/                                # Traffic simulator
│   └── TrafficSimulator.java
└── web/                                      # REST controllers
    ├── GlobalExceptionHandler.java
    ├── SensorController.java
    └── TrafficReadingController.java
```

## Troubleshooting

### Simulator Not Running
Check that:
- `APP_SIMULATOR_ENABLED=true`
- At least one sensor has `ACTIVE` status
- MongoDB is accessible
- Check logs for scheduler errors

### Kafka Events Not Publishing
Verify:
- `APP_KAFKA_ENABLED=true`
- Kafka broker is accessible
- Topics `traffic.reading.events` and `sensor.status.events` exist

### Redis Cache Issues
Ensure:
- Redis is running and accessible
- Connection settings are correct
- Redis is not full (check memory usage)

### MongoDB Connection Errors
Check:
- MongoDB URI is correct
- Database `cityflow` exists
- Network connectivity to MongoDB

## License

Part of the CityFlow project. See LICENSE file in repository root.
