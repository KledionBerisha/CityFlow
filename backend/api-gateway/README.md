# API Gateway

Unified entry point for all CityFlow microservices using Spring Cloud Gateway.

## Overview

The API Gateway provides:
- **Unified API**: Single entry point for all backend services
- **Authentication**: OAuth2/JWT validation (Keycloak)
- **Rate Limiting**: Per-IP rate limiting using Redis
- **CORS**: Cross-origin support for frontend
- **Request Logging**: Correlation IDs and request tracking
- **Load Balancing**: Distribute requests across service instances

## Architecture

```
Client → API Gateway (Port 8000) → {
    /api/routes/** → route-mgmt-service (8081)
    /api/stops/** → route-mgmt-service (8081)
    /api/buses/** → bus-ingestion-service (8082)
    /api/bus-locations/** → bus-ingestion-service (8082)
}
```

## Running Locally

```bash
cd backend/api-gateway
mvn spring-boot:run
```

Gateway will start on http://localhost:8000

## API Routes

### Route Management (Protected)

**Routes**
- `GET /api/routes` - List all routes (requires: `routes_read`)
- `POST /api/routes` - Create route (requires: `routes_write`)
- `GET /api/routes/{id}` - Get route by ID (requires: `routes_read`)

**Stops**
- `GET /api/stops` - List all stops (requires: `stops_read`)
- `POST /api/stops` - Create stop (requires: `stops_write`)
- `GET /api/stops/{id}` - Get stop by ID (requires: `stops_read`)

### Bus Tracking (Protected)

**Buses**
- `GET /api/buses` - List all buses (requires: `bus_read`)
- `POST /api/buses` - Register bus (requires: `bus_write`)
- `GET /api/buses/{id}` - Get bus by ID (requires: `bus_read`)
- `PATCH /api/buses/{id}/status` - Update status (requires: `bus_write`)

**Real-time Locations**
- `GET /api/bus-locations/current` - All current positions (requires: `bus_read`)
- `GET /api/bus-locations/current/{busId}` - Specific bus location (requires: `bus_read`)
- `GET /api/bus-locations/history/{busId}` - Location history (requires: `bus_read`)

## Authentication

### Get Access Token from Keycloak

```bash
curl -X POST http://localhost:8080/realms/cityflow/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=cityflow-client" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin"
```

### Use Token in Requests

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8000/api/buses
```

## Rate Limiting

- **Route Service**: 10 requests/second, burst: 20
- **Bus Service**: 20 requests/second, burst: 40
- Rate limits are per IP address
- Uses Redis for distributed rate limiting

## CORS Configuration

Allowed Origins:
- `http://localhost:3000` (React dev server)
- `http://localhost:3001` (Alternative frontend)

All HTTP methods and headers are allowed for these origins.

## Request Tracking

Every request gets a correlation ID:
- Header: `X-Request-ID`
- Automatically generated if not provided
- Included in all logs and responses

Example:
```
GET /api/buses
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
```

## Configuration

Key environment variables:

```yaml
ROUTE_SERVICE_URL: http://localhost:8081
BUS_SERVICE_URL: http://localhost:8082
OAUTH2_ISSUER_URI: http://localhost:8080/realms/cityflow
SPRING_DATA_REDIS_HOST: localhost
SPRING_DATA_REDIS_PORT: 6379
APP_SECURITY_ENABLED: true
```

## Health & Monitoring

- **Health**: `GET /actuator/health`
- **Info**: `GET /actuator/info`
- **Metrics**: `GET /actuator/metrics`
- **Gateway Routes**: `GET /actuator/gateway/routes`

## Security

### Public Endpoints (No Auth Required)
- `/actuator/health`
- `/actuator/info`

### Protected Endpoints
All API endpoints require valid JWT token with appropriate roles.

### Roles
- `bus_read` - Read bus data
- `bus_write` - Modify bus data
- `routes_read` - Read route data
- `routes_write` - Modify route data
- `stops_read` - Read stop data
- `stops_write` - Modify stop data

## Docker

```bash
# Build image
docker build -t cityflow/api-gateway:latest .

# Run with Docker Compose
docker-compose up api-gateway
```

## Development

To disable security for development:

```yaml
app:
  security:
    enabled: false
```

## Testing Gateway Routes

```bash
# Via Gateway (unified entry point)
curl http://localhost:8000/api/buses
curl http://localhost:8000/api/routes

# Direct to services (bypassing gateway)
curl http://localhost:8082/buses
curl http://localhost:8081/routes
```

## Performance

- **Latency**: < 10ms overhead per request
- **Throughput**: Handles 1000+ req/sec
- **Reactive**: Non-blocking I/O with WebFlux
- **Scalable**: Stateless, can run multiple instances

## Future Enhancements

- [ ] Circuit breaker pattern (Resilience4j)
- [ ] Request/response transformation
- [ ] API versioning
- [ ] GraphQL gateway
- [ ] WebSocket support
- [ ] Distributed tracing (Zipkin/Jaeger)
