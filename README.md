# CityFlow
Real-Time City Transport & Traffic Monitoring System

## Overview
- Real-time monitoring of city traffic and public transit using sensors, GPS feeds, or simulators.
- Detects congestion, incidents, vehicle positions, delays, and active routes; sends alerts when conditions change.
- Dashboards with maps and charts; short-term traffic prediction (10–30 minutes) using ML.

## Përshkrim
Ky sistem monitoron në kohë reale trafikun dhe transportin publik në një qytet. Ai përdor sensorë, GPS të autobusëve ose simulatorë për të mbledhur të dhëna dhe i përpunon ato për të analizuar lëvizjen e mjeteve, bllokimet e trafikut dhe kohët e mbërritjes.

Çfarë bën sistemi?
- Mbledh të dhëna në kohë reale për trafikun (shpejtësi, numër makinash, bllokime).
- Përpunon të dhënat për të identifikuar zona me trafik të lartë.
- Monitoron autobusët: vendndodhjen, vonesat, rrugët aktive.
- Jep sinjalizime kur ndodhin ngjarje (aksident, zbritje shpejtësie, bllokim).
- Vizualizon të dhënat në një dashboard me harta dhe graﬁkë.
- Parashikon trafikun për 10–30 minuta më vonë (me algoritme ML).

## Dev stack
- Frontend: React
- Backend: Spring Boot microservices (gateway, bus ingestion, route mgmt)
- Microservices runtime: Docker (dev) + Kubernetes
- Event streaming: Apache Kafka (ZooKeeper) + optional Redpanda Console
- Datastores: PostgreSQL + MongoDB
- Data/ML: Python + TensorFlow (Spark/Airflow optional for pipelines)
- Security: Keycloak (OIDC/OAuth2) + JWT

## Quick start (dev)
1) `docker compose up -d`
   - **API Gateway**: `http://localhost:8000` ⭐ Main entry point for all APIs
   - **Route Service**: `http://localhost:8081`
   - **Bus Ingestion Service**: `http://localhost:8082`
   - **Keycloak**: `http://localhost:8080` (admin/admin)
   - **Kafka brokers**: `localhost:9093`
   - **Postgres**: `localhost:5433` (user/pass: `kledionberisha` / `kledion123`, db: `cityflow`)
   - **MongoDB**: `localhost:27017`
   - **Redis**: `localhost:6379`

2) Test the system:
   ```bash
   curl http://localhost:8000/api/buses
   curl http://localhost:8000/api/bus-locations/current
   curl http://localhost:8000/api/routes
   ```

## Services

### Route Management Service
- Location: `backend/route-mgmt-service`
- Build/run: `mvn clean package` then `mvn spring-boot:run`
- DB: Postgres `cityflow` on `localhost:5432` with user/password `kledionberisha` / `kledion123`
- Endpoints:
  - `POST /routes` (create route: code, name, active)
  - `GET /routes` (list)
  - `GET /routes/{id}` (fetch by UUID)
  - `POST /stops` (create stop: code?, name, lat, lon, terminal, active)
  - `GET /stops` / `GET /stops/{id}` (list/fetch stops)
  - `PUT /routes/{id}/stops` (replace ordered stops for a route; enforces unique sequence and stop)
  - `GET /routes/{id}/stops` (list ordered stops for a route)
  - `PUT /routes/{id}/schedules` (replace schedules for a route)
  - `GET /routes/{id}/schedules` (list schedules for a route)
- Pagination: max page size capped at 100 for list endpoints

### Security
- Resource server with JWT (Keycloak)
- Default issuer URI: `http://localhost:8080/realms/cityflow` (override with `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`)
- Disable auth (tests/dev only): set `APP_SECURITY_ENABLED=false`
- Roles expected (Keycloak `realm_access.roles`):
  - `routes_read`: GET routes/stops
  - `routes_write`: POST/PUT/DELETE routes, route-stops, schedules
  - `stops_read`: GET stops
  - `stops_write`: POST/PUT/DELETE stops

### Observability
- Actuator health/info exposed (`/actuator/health`, `/actuator/info`)
- Request logging with per-request `requestId` MDC
- Schema (Flyway-managed):
  - routes, stops, route_stops, schedules, buses, users
  - Route fields map to DB columns: `route_code`, `route_name`, `is_active`

---

### Bus Ingestion Service ✨ NEW
- **Location**: `backend/bus-ingestion-service`
- **Port**: `8082`
- **Technology**: Spring Boot WebFlux (Reactive), MongoDB, Redis, Kafka
- **Purpose**: Real-time bus GPS tracking, location simulation, and event streaming

#### Features
- ✅ Bus fleet management (CRUD)
- ✅ Real-time GPS location tracking
- ✅ Built-in GPS simulator for development
- ✅ Redis caching for fast location queries
- ✅ MongoDB for historical location storage
- ✅ Kafka event streaming for downstream consumers

#### Key Endpoints
- `POST /buses` - Register new bus
- `GET /buses` - List all buses
- `GET /buses/route/{routeId}` - Get buses on specific route
- `PATCH /buses/{id}/status` - Update bus status (ACTIVE/IDLE/MAINTENANCE/OFFLINE)
- `GET /bus-locations/current` - Get all current bus locations (from cache)
- `GET /bus-locations/current/{busId}` - Get current location for specific bus
- `GET /bus-locations/current/route/{routeId}` - Get real-time locations for route
- `GET /bus-locations/history/{busId}` - Get historical location data
- `GET /bus-locations/recent` - Poll for recent updates

#### GPS Simulator
The service includes a built-in GPS simulator that automatically:
- Generates realistic bus movements for all `ACTIVE` buses
- Updates positions every 5 seconds (configurable)
- Stores in MongoDB and caches in Redis
- Publishes events to Kafka topic: `bus.location.events`

On startup, 5 sample buses are created (3 ACTIVE, 1 IDLE, 1 MAINTENANCE).

#### Kafka Events
**Topics:**
- `bus.location.events` - Real-time location updates (5 partitions)
- `bus.status.events` - Bus status changes (3 partitions)

#### Configuration
```yaml
app:
  simulator:
    enabled: true      # GPS simulation
    interval-ms: 5000  # Update frequency
    speed-kmh: 40      # Average speed
  redis:
    ttl-seconds: 300   # Cache TTL (5 min)
```

#### Security
- OAuth2/JWT (Keycloak)
- Roles: `bus_read`, `bus_write`
- Dev mode: Set `APP_SECURITY_ENABLED=false`

See detailed documentation: [backend/bus-ingestion-service/README.md](backend/bus-ingestion-service/README.md)

---

### API Gateway ⭐ NEW
- **Location**: `backend/api-gateway`
- **Port**: `8000`
- **Technology**: Spring Cloud Gateway (Reactive), Redis
- **Purpose**: Unified API entry point with routing, security, and rate limiting

#### Features
- ✅ Smart routing to microservices
- ✅ Rate limiting (IP-based, Redis-backed)
- ✅ CORS support for frontend
- ✅ OAuth2/JWT authentication (Keycloak)
- ✅ Request correlation IDs and logging
- ✅ Health checks and route inspection

#### API Routes
All requests go through `http://localhost:8000`:
- `/api/buses/*` → Bus Ingestion Service (8082)
- `/api/bus-locations/*` → Bus Ingestion Service (8082)
- `/api/routes/*` → Route Management Service (8081)
- `/api/stops/*` → Route Management Service (8081)

#### Rate Limits
- `/api/buses/*`: 10 requests/sec per IP
- `/api/bus-locations/*`: 20 requests/sec per IP
- `/api/routes/*`: 10 requests/sec per IP

#### Example Usage
```bash
# Get all buses through gateway
curl http://localhost:8000/api/buses

# Get real-time locations
curl http://localhost:8000/api/bus-locations/current

# Get routes
curl http://localhost:8000/api/routes

# Health check
curl http://localhost:8000/actuator/health

# View configured routes
curl http://localhost:8000/actuator/gateway/routes
```

#### Security
- OAuth2/JWT (Keycloak)
- Public paths (no auth required): `/actuator/health`, `/actuator/info`
- Dev mode: Set `APP_SECURITY_ENABLED=false`

See detailed documentation: [backend/api-gateway/README.md](backend/api-gateway/README.md)
