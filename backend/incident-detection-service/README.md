# Incident Detection Service 🚨

Real-time incident detection service for CityFlow. Analyzes traffic and bus events to automatically detect accidents, severe congestion, breakdowns, and other incidents.

## Overview

The Incident Detection Service:
- Consumes real-time events from Kafka (traffic readings, bus locations)
- Applies detection algorithms to identify incidents
- Stores detected incidents in MongoDB
- Publishes incident events to Kafka for downstream consumers
- Provides REST APIs for incident management

## Technology Stack

- **Spring Boot 3.2.2** with WebFlux (reactive)
- **MongoDB** - Incident storage
- **Kafka** - Event consumption & production
- **OAuth2/JWT** - Security via Keycloak
- **Java 17** - Runtime

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- MongoDB running on `localhost:27017`
- Kafka running on `localhost:9093`
- Traffic and Bus services publishing events

### Build & Run

```bash
cd backend/incident-detection-service

# Build
mvn clean package

# Run locally
mvn spring-boot:run

# Run with Docker
docker build -t incident-detection-service .
docker run -p 8086:8086 \
  -e SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/cityflow \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  incident-detection-service
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8086` | HTTP server port |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/cityflow` | MongoDB connection |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | Kafka brokers |
| `OAUTH2_ISSUER_URI` | `http://localhost:8080/realms/cityflow` | Keycloak issuer |
| `APP_SECURITY_ENABLED` | `true` | Enable/disable OAuth2 security |
| `APP_KAFKA_ENABLED` | `true` | Enable/disable Kafka consuming |

## API Endpoints

### Get All Incidents
```bash
GET /incidents
```

Returns all detected incidents.

### Get Active Incidents
```bash
GET /incidents/active
```

Returns incidents that are DETECTED, CONFIRMED, or IN_PROGRESS.

**Example Response:**
```json
[
  {
    "id": "65a1234567890abcdef12345",
    "incidentCode": "INC-2026-0042",
    "type": "ACCIDENT",
    "severity": "CRITICAL",
    "status": "DETECTED",
    "title": "Sudden Speed Drop - Potential Accident",
    "description": "Sudden 68% speed drop detected (52.3 → 16.7 km/h)",
    "latitude": 42.6521,
    "longitude": 21.1657,
    "roadSegmentId": "ROAD-SEG-003",
    "sourceId": "sensor-003",
    "sourceType": "TRAFFIC_SENSOR",
    "detectedAt": "2026-01-09T14:30:00Z",
    "detectionMethod": "SPEED_DROP_ANALYSIS",
    "confidence": 0.85,
    "impactRadiusKm": 0.5,
    "createdAt": "2026-01-09T14:30:05Z"
  }
]
```

### Get Recent Incidents
```bash
GET /incidents/recent?hoursBack=24
```

Returns incidents from the last N hours (default: 24).

### Get by Status
```bash
GET /incidents/status/{status}
```

Filter by status: `DETECTED`, `CONFIRMED`, `IN_PROGRESS`, `RESOLVED`, `DISMISSED`.

### Get by Type
```bash
GET /incidents/type/{type}
```

Filter by type: `ACCIDENT`, `SEVERE_CONGESTION`, `ROAD_CLOSURE`, `BUS_BREAKDOWN`, `SENSOR_MALFUNCTION`, etc.

### Get by Severity
```bash
GET /incidents/severity/{severity}
```

Filter by severity: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.

### Update Incident Status
```bash
PATCH /incidents/{id}/status?status=CONFIRMED
```

Update the status of an incident.

### Delete Incident
```bash
DELETE /incidents/{id}
```

Delete an incident (for false positives).

## Detection Algorithms

### 1. Sudden Speed Drop Detection

**Purpose:** Detect potential accidents from sudden traffic speed changes

**Algorithm:**
- Monitors rolling average speed from last 5 readings
- Triggers when:
  - Speed drops > 50%
  - Current speed < 20 km/h
  - Previous average > 40 km/h

**Confidence Scoring:**
- Base: 0.60
- +0.20 if drop > 70%
- +0.10 if current speed < 10 km/h
- Max: 0.95

**Example:**
```
Before: 52.3 km/h → After: 16.7 km/h (68% drop)
→ ACCIDENT detected with 85% confidence
```

### 2. Severe Congestion Detection

**Purpose:** Identify abnormal congestion patterns

**Triggers:**
- Congestion level = SEVERE
- Road occupancy > 90%

**Impact Assessment:**
- Estimated delay based on average speed
- Affected vehicle count from sensor

### 3. Sensor Incident Flag

**Purpose:** Capture incidents directly reported by sensors

**Triggers:**
- Sensor's `incidentDetected` flag = true

**Confidence:** 0.95 (high trust in sensor)

### 4. Bus Abnormality Detection

**Purpose:** Detect bus breakdowns or sudden stops

**Algorithm:**
- Compares consecutive bus positions
- Triggers when:
  - Previous speed > 30 km/h
  - Current speed < 5 km/h
  - Time difference < 30 seconds

**Severity:** MEDIUM

## Kafka Integration

### Topics Consumed

**1. `traffic.reading.events`**
- Source: Traffic Ingestion Service
- Concurrency: 3 consumers
- Data: Speed, congestion, vehicle count, incident flags

**2. `bus.location.events`**
- Source: Bus Ingestion Service
- Concurrency: 2 consumers
- Data: Bus position, speed, heading

### Topic Published

**`incident.events`**
- Partitions: 3
- Published when:
  - New incident detected
  - Incident status updated

**Event Schema:**
```json
{
  "eventId": "uuid",
  "incidentId": "mongodb-id",
  "incidentCode": "INC-2026-0042",
  "type": "ACCIDENT",
  "severity": "CRITICAL",
  "status": "DETECTED",
  "title": "string",
  "description": "string",
  "latitude": 42.6521,
  "longitude": 21.1657,
  "roadSegmentId": "ROAD-SEG-003",
  "sourceId": "sensor-003",
  "sourceType": "TRAFFIC_SENSOR",
  "detectedAt": "2026-01-09T14:30:00Z",
  "confidence": 0.85,
  "affectedVehicles": 45,
  "affectedBuses": 2,
  "eventTimestamp": "2026-01-09T14:30:05Z"
}
```

## Incident Types

| Type | Description | Typical Severity |
|------|-------------|------------------|
| `ACCIDENT` | Traffic accident detected | HIGH/CRITICAL |
| `SEVERE_CONGESTION` | Abnormal severe congestion | HIGH |
| `ROAD_CLOSURE` | Road blocked/closed | HIGH |
| `BUS_BREAKDOWN` | Bus malfunction/offline | MEDIUM |
| `SENSOR_MALFUNCTION` | Sensor offline/error | LOW/MEDIUM |
| `WEATHER_RELATED` | Weather-induced incident | MEDIUM/HIGH |
| `CONSTRUCTION` | Road construction/maintenance | LOW/MEDIUM |
| `SPECIAL_EVENT` | Event causing traffic impact | MEDIUM |
| `UNKNOWN` | Unclassified incident | MEDIUM |

## MongoDB Schema

### Collection: `incidents`

**Indexes:**
- `detectedAt` (descending)
- `{ latitude, longitude, detectedAt }` (compound, geospatial queries)
- `{ type, severity, detectedAt }` (compound, filtering)
- `status` (filtering active incidents)

**TTL:** Consider setting TTL index for auto-cleanup of old incidents

## Security

**Required Roles:**
- `incident_read` or `SCOPE_incident_read` - Read access to incidents

**Public Endpoints:**
- `/actuator/health`
- `/actuator/info`

**Dev Mode:**
```bash
APP_SECURITY_ENABLED=false mvn spring-boot:run
```

## Health & Monitoring

```bash
curl http://localhost:8086/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "mongo": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

## Integration with CityFlow Ecosystem

### Upstream Services
- **Traffic Ingestion Service** → Provides traffic sensor data
- **Bus Ingestion Service** → Provides bus location data

### Downstream Consumers
- **Notification Service** → Sends alerts based on incident events
- **Analytics Service** → Incorporates incident data into metrics
- **Frontend Dashboard** → Displays incidents on map

## Performance Characteristics

- **Event Processing:** ~500 events/second (per consumer thread)
- **Detection Latency:** < 100ms per event
- **Memory Buffers:** Last 50 readings per sensor
- **Incident Deduplication:** 5-15 minute windows per detection type

## Configuration

### Detection Sensitivity

Adjust thresholds in `IncidentDetectionService.java`:

```java
// Speed drop threshold
if (speedDrop > 50 && currentSpeed < 20)

// Congestion threshold
if ("SEVERE".equals(event.getCongestionLevel()) && occupancy > 0.9)

// Bus abnormality threshold
if (previousSpeed > 30 && currentSpeed < 5)
```

### Deduplication Windows

```java
recentIncidents.get(key);
Instant.now().minus(10, ChronoUnit.MINUTES)
```

## Future Enhancements

1. **Machine Learning Detection**
   - Train models on historical incident patterns
   - Anomaly detection using autoencoders
   - Predictive incident probability

2. **Multi-Source Correlation**
   - Correlate incidents across multiple sensors
   - Aggregate confidence from multiple sources
   - Automatic incident confirmation

3. **Geospatial Queries**
   - Find incidents within radius
   - Impact zone calculation
   - Route around incidents

4. **Incident Resolution Tracking**
   - Integration with emergency services
   - Estimated resolution time
   - Real-time status updates

5. **Historical Analysis**
   - Incident hotspot identification
   - Time-of-day patterns
   - Seasonal trends

## Troubleshooting

### No Incidents Being Detected

**Check:**
- Kafka consumers are receiving events (check logs)
- Traffic/Bus services are publishing events
- Detection thresholds may be too strict
- MongoDB connection is working

### Too Many False Positives

**Adjust:**
- Increase confidence thresholds
- Tune detection algorithm parameters
- Extend deduplication windows

### Kafka Consumer Lag

**Solutions:**
- Increase consumer concurrency
- Optimize detection algorithms
- Scale horizontally (multiple instances)

## License

Part of the CityFlow project. See LICENSE file in repository root.
