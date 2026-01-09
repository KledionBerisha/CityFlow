
# Analytics Service 📊

Real-time data aggregation and analytics service for CityFlow. Consumes events from traffic and bus services to provide city-wide and road-segment-level insights.

## Overview

The Analytics Service is a reactive microservice that:
- Consumes real-time events from Kafka (traffic readings, bus locations)
- Aggregates data at multiple levels (city-wide, road segment)
- Calculates performance scores and metrics
- Caches analytics in Redis for fast access
- Provides REST APIs for dashboard consumption

## Technology Stack

- **Spring Boot 3.2.2** with WebFlux (reactive)
- **Redis** - Metrics caching
- **Kafka Consumer** - Event stream processing
- **OAuth2/JWT** - Security via Keycloak
- **Java 17** - Runtime

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Redis running on `localhost:6379`
- Kafka running on `localhost:9093`
- Traffic and Bus services publishing events

### Build & Run

```bash
cd backend/analytics-service

# Build
mvn clean package

# Run locally
mvn spring-boot:run

# Run with Docker
docker build -t analytics-service .
docker run -p 8084:8084 \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  analytics-service
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8084` | HTTP server port |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | Kafka brokers |
| `OAUTH2_ISSUER_URI` | `http://localhost:8080/realms/cityflow` | Keycloak issuer |
| `APP_SECURITY_ENABLED` | `true` | Enable/disable OAuth2 security |
| `APP_KAFKA_ENABLED` | `true` | Enable/disable Kafka consuming |

## API Endpoints

### Get City-Wide Metrics

```bash
GET /analytics/city
```

Returns aggregated metrics for the entire city.

**Response:**
```json
{
  "timestamp": "2026-01-09T14:30:00Z",
  "totalActiveBuses": 25,
  "totalActiveSensors": 7,
  "totalRoadSegments": 7,
  "cityAverageSpeed": 48.5,
  "totalVehiclesDetected": 234,
  "congestionLevelCounts": {
    "FREE_FLOW": 4,
    "LIGHT": 2,
    "MODERATE": 1,
    "HEAVY": 0,
    "SEVERE": 0
  },
  "busesOnTime": 18,
  "busesDelayed": 7,
  "averageDelay": 2.5,
  "activeIncidents": 0,
  "cityTrafficScore": 82.5
}
```

### Get All Road Segment Metrics

```bash
GET /analytics/segments
```

Returns metrics for all road segments being monitored.

**Response:**
```json
[
  {
    "roadSegmentId": "ROAD-SEG-001",
    "timestamp": "2026-01-09T14:30:00Z",
    "averageSpeed": 52.3,
    "totalVehicles": 45,
    "averageOccupancy": 0.35,
    "congestionLevel": "FREE_FLOW",
    "queueLength": 2,
    "activeBusCount": 0,
    "averageBusDelay": 0.0,
    "trafficFlowScore": 85.0,
    "incidentCount": 0
  },
  ...
]
```

### Get Specific Road Segment Metrics

```bash
GET /analytics/segments/{roadSegmentId}
```

Returns detailed metrics for a specific road segment.

**Example:**
```bash
GET /analytics/segments/ROAD-SEG-001
```

## Data Models

### CityWideMetrics

Aggregated metrics for the entire city:

```java
{
  "timestamp": "instant",
  "totalActiveBuses": "integer",
  "totalActiveSensors": "integer",
  "totalRoadSegments": "integer",
  "cityAverageSpeed": "double",           // km/h
  "totalVehiclesDetected": "integer",
  "congestionLevelCounts": {
    "FREE_FLOW": 10,
    "MODERATE": 5,
    ...
  },
  "busesOnTime": "integer",
  "busesDelayed": "integer",
  "averageDelay": "double",               // minutes
  "activeIncidents": "integer",
  "cityTrafficScore": "double"            // 0-100
}
```

### RoadSegmentMetrics

Metrics for individual road segments:

```java
{
  "roadSegmentId": "string",
  "timestamp": "instant",
  "averageSpeed": "double",               // km/h
  "totalVehicles": "integer",
  "averageOccupancy": "double",           // 0.0-1.0
  "congestionLevel": "string",            // FREE_FLOW, LIGHT, MODERATE, HEAVY, SEVERE
  "queueLength": "integer",
  "activeBusCount": "integer",
  "averageBusDelay": "double",            // minutes
  "trafficFlowScore": "double",           // 0-100
  "incidentCount": "integer"
}
```

## Kafka Event Consumption

### Traffic Reading Events

**Topic:** `traffic.reading.events`

The service consumes traffic data and aggregates:
- Average speeds per road segment
- Vehicle counts
- Congestion levels
- Incident detections

### Bus Location Events

**Topic:** `bus.location.events`

The service consumes bus location data and tracks:
- Active bus counts per route
- Bus speeds and delays
- Route performance

### Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      group-id: analytics-service
      auto-offset-reset: latest
      enable-auto-commit: true
      max-poll-records: 100
```

- **Group ID:** `analytics-service`
- **Concurrency:** 3 threads per topic
- **Offset Strategy:** Latest (real-time only)

## Analytics Algorithms

### Traffic Flow Score Calculation

The traffic flow score (0-100) is calculated based on:

1. **Speed Factor** (40% weight)
   - Higher speeds = better score
   - Penalty for very slow (<20 km/h) or excessive (>80 km/h) speeds

2. **Occupancy Factor** (30% weight)
   - Moderate occupancy (30-60%) is optimal
   - High occupancy (>80%) significantly reduces score

3. **Congestion Factor** (30% weight)
   - FREE_FLOW: +10 points
   - LIGHT: +5 points
   - MODERATE: -10 points
   - HEAVY: -25 points
   - SEVERE: -40 points

**Formula:**
```
score = 100
score -= speedPenalty(averageSpeed)
score -= occupancyPenalty(occupancy)
score -= congestionPenalty(congestionLevel)
score = max(0, min(100, score))
```

### City Traffic Score

The city-wide traffic score aggregates:

1. **City Average Speed**
   - Target: 45-60 km/h
   - Penalties for speeds < 30 km/h

2. **Congestion Distribution**
   - Severe congestion: -5 points each
   - Heavy congestion: -3 points each

3. **Incident Impact**
   - Active incidents: -2 points each

4. **Bus Performance Bonus**
   - On-time rate > 70%: bonus points

## Redis Cache Structure

### Cache Keys

- **Road Segment Metrics:** `analytics:segment:{roadSegmentId}`
- **City Metrics:** `analytics:city:metrics`

### Cache TTL

- **Default:** 5 minutes (300 seconds)
- Automatically refreshed on new events

### Example Redis Query

```bash
# Get city metrics
redis-cli GET "analytics:city:metrics"

# Get specific segment
redis-cli GET "analytics:segment:ROAD-SEG-001"

# List all segments
redis-cli KEYS "analytics:segment:*"
```

## Aggregation Logic

### In-Memory Buffering

The service maintains in-memory buffers for:
- Recent traffic readings per road segment (last 100)
- Recent bus locations per route (last 100)
- Incident counts per segment

### Aggregation Triggers

Aggregation occurs on every new event:
1. **Traffic Event** → Update segment metrics → Update city metrics
2. **Bus Event** → Update city metrics

### Data Retention

- **Memory buffers:** Keep last 50-100 readings per segment
- **Redis cache:** 5-minute TTL
- **Real-time only:** No historical data storage

## Security

### OAuth2/JWT

The service acts as an OAuth2 Resource Server using Keycloak.

**Required Roles:**
- `analytics_read` or `SCOPE_analytics_read` - Read access to analytics

**Public Endpoints** (no auth):
- `/actuator/health`
- `/actuator/info`

### Development Mode

Disable security for local testing:

```bash
export APP_SECURITY_ENABLED=false
mvn spring-boot:run
```

## Health & Monitoring

### Actuator Endpoints

- `GET /actuator/health` - Service health status
- `GET /actuator/info` - Service information
- `GET /actuator/metrics` - Prometheus metrics

### Health Checks

The service reports health based on:
- Redis connectivity
- Kafka consumer status
- Application status

Example:
```bash
curl http://localhost:8084/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

## Integration with Other Services

### Upstream Services

**Traffic Ingestion Service:**
- Publishes: `traffic.reading.events`
- Provides: Sensor data, congestion levels, incidents

**Bus Ingestion Service:**
- Publishes: `bus.location.events`
- Provides: Bus locations, speeds, routes

### Downstream Consumers

**Frontend Dashboard:**
- Consumes: City and segment metrics APIs
- Displays: Real-time analytics, heatmaps, charts

**Notification Service (Future):**
- Could subscribe to analytics changes
- Send alerts when scores drop below thresholds

## Development

### Running Tests

```bash
mvn test
```

### Building Docker Image

```bash
docker build -t cityflow/analytics-service:latest .
```

### Code Structure

```
src/main/java/com/cityflow/analytics/
├── AnalyticsServiceApplication.java     # Main entry point
├── config/                               # Configuration classes
│   ├── KafkaConfig.java                 # Kafka consumer config
│   ├── RedisConfig.java                 # Redis template config
│   └── SecurityConfig.java              # OAuth2 security config
├── consumer/                             # Kafka event consumers
│   ├── BusEventConsumer.java
│   └── TrafficEventConsumer.java
├── dto/                                  # Event DTOs
│   ├── BusLocationEvent.java
│   └── TrafficReadingEvent.java
├── model/                                # Domain models
│   ├── CityWideMetrics.java
│   └── RoadSegmentMetrics.java
├── service/                              # Business logic
│   └── AnalyticsAggregationService.java # Core aggregation engine
└── web/                                  # REST controllers
    ├── AnalyticsController.java
    └── GlobalExceptionHandler.java
```

## Performance Considerations

### Throughput

- **Event Processing:** ~1000 events/second per consumer thread
- **Concurrency:** 3 threads per Kafka topic (6 total)
- **Aggregation Latency:** < 50ms per event

### Memory Usage

- **Buffer Size:** ~100 events × 8 topics = 800 events in memory
- **Estimated RAM:** ~500MB with JVM heap

### Scalability

To scale horizontally:
1. Deploy multiple instances with same `group-id`
2. Kafka will automatically partition load
3. Each instance maintains its own in-memory state
4. Redis acts as shared cache layer

## Troubleshooting

### No Metrics Returned

Check that:
- Kafka consumers are receiving events
- Traffic and Bus services are publishing events
- Redis is accessible
- Check logs for aggregation errors

### Kafka Consumer Not Starting

Verify:
- `APP_KAFKA_ENABLED=true`
- Kafka broker is accessible
- Topics `traffic.reading.events` and `bus.location.events` exist
- Consumer group `analytics-service` has proper permissions

### Redis Connection Errors

Ensure:
- Redis is running and accessible
- Connection settings are correct
- Redis is not full (check memory usage)

### Inaccurate Metrics

Consider:
- Event processing delays (check Kafka lag)
- Buffer sizes may need tuning
- Aggregation algorithms may need calibration
- Check for data quality issues in source services

## Future Enhancements

1. **Historical Analytics**
   - Store aggregated metrics in time-series DB
   - Trend analysis and reporting

2. **Predictive Analytics**
   - ML-based traffic prediction
   - Anomaly detection

3. **Advanced Scoring**
   - Customizable weighting factors
   - Route-specific benchmarks

4. **Real-Time Alerts**
   - Threshold-based notifications
   - Trend change detection

5. **WebSocket Support**
   - Push analytics to dashboard
   - Eliminate polling

## License

Part of the CityFlow project. See LICENSE file in repository root.
