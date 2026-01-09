# Notification Service 🔔

Real-time notification and alerting service for CityFlow. Monitors events from traffic and bus services and sends notifications via WebSocket and other channels.

## Overview

The Notification Service:
- Consumes events from Kafka (traffic, bus, analytics)
- Detects threshold violations and critical events
- Sends real-time notifications via WebSocket
- Stores notification history in MongoDB
- Provides REST APIs for notification management

## Technology Stack

- **Spring Boot 3.2.2** with WebFlux (reactive)
- **WebSocket** (STOMP) - Real-time push notifications
- **MongoDB** - Notification history and rules
- **Redis** - Subscription caching
- **Kafka Consumer** - Event processing
- **OAuth2/JWT** - Security via Keycloak
- **Java 17** - Runtime

## Quick Start

### Build & Run

```bash
cd backend/notification-service

# Build
mvn clean package

# Run locally
mvn spring-boot:run
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8085` | HTTP server port |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/cityflow` | MongoDB connection |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | Kafka brokers |
| `APP_SECURITY_ENABLED` | `true` | Enable/disable OAuth2 |

## API Endpoints

### Get Recent Notifications
```bash
GET /notifications/recent?limit=50
```

### Get Unread Notifications
```bash
GET /notifications/recent?limit=50
# Returns only unread notifications
```

### Get by Priority
```bash
GET /notifications/priority/CRITICAL
```

### Get Since Timestamp
```bash
GET /notifications/since?since=2026-01-09T10:00:00Z
```

### Mark as Read
```bash
PATCH /notifications/{id}/read
```

### Delete Notification
```bash
DELETE /notifications/{id}
```

## WebSocket Connection

### Connect to WebSocket

```javascript
const socket = new SockJS('http://localhost:8085/ws/notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to notifications
    stompClient.subscribe('/topic/notifications', function(message) {
        const notification = JSON.parse(message.body);
        console.log('New notification:', notification);
        displayNotification(notification);
    });
});
```

### Notification Format

```json
{
  "id": "65a1234567890abcdef12345",
  "type": "TRAFFIC_CONGESTION",
  "priority": "WARNING",
  "title": "HEAVY Congestion",
  "message": "HEAVY congestion at SENSOR-001 (Speed: 18.5 km/h, Vehicles: 85)",
  "source": "sensor-id-123",
  "sourceType": "TRAFFIC",
  "timestamp": "2026-01-09T14:30:00Z",
  "read": false,
  "sent": true,
  "sentViaWebSocket": true
}
```

## Notification Types

- `TRAFFIC_CONGESTION` - Heavy or severe congestion detected
- `TRAFFIC_INCIDENT` - Incident detected by sensors
- `BUS_DELAY` - Significant bus delays
- `BUS_BREAKDOWN` - Bus offline/breakdown
- `CITY_SCORE_DROP` - City traffic score dropped significantly
- `SENSOR_OFFLINE` - Traffic sensor went offline
- `ROUTE_DISRUPTION` - Route service disruption
- `SYSTEM_ALERT` - System-level alerts

## Priority Levels

- **INFO** - Informational, no action required
- **WARNING** - Needs attention soon
- **CRITICAL** - Urgent action required

## Event Processing

### Traffic Events

The service monitors:
- **Severe/Heavy Congestion** → WARNING/CRITICAL notification
- **Incident Detection** → CRITICAL notification
- **Sensor Offline** → WARNING notification

### Alert Rules (Future)

Alert rules can be configured to:
- Set custom thresholds
- Define notification channels
- Filter by road segments/routes
- Schedule active hours

## Kafka Topics Consumed

- `traffic.reading.events` - Traffic sensor data
- `bus.location.events` - Bus location updates
- `sensor.status.events` - Sensor status changes

## MongoDB Collections

### notifications
- Stores all notifications
- Indexed on `timestamp` (desc) and `userId`
- Retention: Configurable

### alert_rules
- Stores alerting rules
- Enables custom threshold configuration

## Security

**Required Roles:**
- `notification_read` or `SCOPE_notification_read` - Read notifications
- `notification_write` or `SCOPE_notification_write` - Delete notifications

**Public Endpoints:**
- `/actuator/health`, `/actuator/info`
- `/ws/**` - WebSocket endpoint

## Health & Monitoring

```bash
curl http://localhost:8085/actuator/health
```

## Testing WebSocket with curl

```bash
# Using wscat
npm install -g wscat
wscat -c ws://localhost:8085/ws/notifications
```

## Integration

### Frontend Integration

```javascript
// React example
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const connectToNotifications = () => {
  const socket = new SockJS('http://localhost:8000/ws/notifications');
  const client = Stomp.over(socket);
  
  client.connect({}, () => {
    client.subscribe('/topic/notifications', (message) => {
      const notification = JSON.parse(message.body);
      showToast(notification);
    });
  });
};
```

## License

Part of the CityFlow project. See LICENSE file in repository root.
