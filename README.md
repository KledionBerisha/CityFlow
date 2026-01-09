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
   - **API Gateway**: `http://localhost:8000` ⭐ Main entry point
   - **Kafka brokers**: `localhost:9093`
   - **Postgres**: `localhost:5433` (user/pass: `kledionberisha` / `kledion123`, db: `cityflow`)
   - **MongoDB**: `localhost:27017`
   - **Redis**: `localhost:6379`
   - **Keycloak**: `http://localhost:8080` (admin/admin)
   - **Route Service**: `http://localhost:8081`
   - **Bus Ingestion Service**: `http://localhost:8082`
   - **Traffic Ingestion Service**: `http://localhost:8083`

2) Test endpoints:
   ```bash
   curl http://localhost:8000/api/routes
   curl http://localhost:8000/api/buses
   curl http://localhost:8000/api/bus-locations/current
   curl http://localhost:8000/api/sensors
   curl http://localhost:8000/api/traffic/current
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

### Traffic Ingestion Service 🚦 NEW
- **Location**: `backend/traffic-ingestion-service`
- **Port**: `8083`
- **Technology**: Spring Boot WebFlux (Reactive), MongoDB, Redis, Kafka
- **Purpose**: Real-time traffic monitoring from sensors and traffic data simulation

#### Features
- ✅ Traffic sensor management (CRUD)
- ✅ Real-time traffic data collection
- ✅ Built-in traffic simulator for development
- ✅ Redis caching for fast queries
- ✅ MongoDB for historical data storage
- ✅ Kafka event streaming
- ✅ Congestion level detection (5 levels)

#### Key Endpoints
- `POST /sensors` - Register new traffic sensor
- `GET /sensors` - List all sensors
- `GET /sensors/{id}` - Get sensor details
- `GET /sensors/status/{status}` - Filter by status (ACTIVE/INACTIVE/MAINTENANCE/ERROR/OFFLINE)
- `PATCH /sensors/{id}/status` - Update sensor status
- `GET /traffic/current` - Get all current traffic readings (from cache)
- `GET /traffic/current/{sensorId}` - Get current reading for specific sensor
- `GET /traffic/history/{sensorId}` - Get historical traffic data
- `GET /traffic/road-segment/{roadSegmentId}` - Get traffic for road segment
- `GET /traffic/recent?minutes=5` - Get recent traffic updates
- `GET /traffic/congestion/{level}` - Filter by congestion (FREE_FLOW/LIGHT/MODERATE/HEAVY/SEVERE)
- `GET /traffic/incidents` - Get readings with detected incidents

#### Traffic Simulator
The service includes a built-in traffic simulator that automatically:
- Generates realistic traffic readings for all `ACTIVE` sensors
- Updates data every 10 seconds (configurable)
- Simulates peak hours (7-9 AM, 5-7 PM) with heavier congestion
- Varies by time of day (light traffic at night)
- Stores in MongoDB and caches in Redis
- Publishes events to Kafka topic: `traffic.reading.events`

On startup, 8 sample sensors are created at various city locations.

#### Kafka Events
**Topics:**
- `traffic.reading.events` - Real-time traffic readings (5 partitions)
- `sensor.status.events` - Sensor status changes (3 partitions)

#### Traffic Metrics
Each reading includes:
- Average speed (km/h)
- Vehicle count
- Road occupancy (%)
- Congestion level (5 levels)
- Queue length
- Environmental data (temperature, weather)
- Incident detection flag

#### Configuration
```yaml
app:
  simulator:
    enabled: true      # Traffic simulation
    interval-ms: 10000 # Update frequency (10s)
  redis:
    ttl-seconds: 300   # Cache TTL (5 min)
```

#### Security
- OAuth2/JWT (Keycloak)
- Roles: `traffic_read`, `traffic_write`
- Dev mode: Set `APP_SECURITY_ENABLED=false`

See detailed documentation: [backend/traffic-ingestion-service/README.md](backend/traffic-ingestion-service/README.md)

---

### API Gateway ⭐
- **Location**: `backend/api-gateway`
- **Port**: `8000`
- **Technology**: Spring Cloud Gateway (Reactive), Redis
- **Purpose**: Unified API entry point with routing, security, and rate limiting

#### Routes
All API requests go through the gateway:
- `/api/routes/*`, `/api/stops/*` → Route Management Service (8081)
- `/api/buses/*`, `/api/bus-locations/*` → Bus Ingestion Service (8082)
- `/api/sensors/*`, `/api/traffic/*` → Traffic Ingestion Service (8083)

#### Features
- ✅ Smart routing to microservices
- ✅ Rate limiting (IP-based, Redis-backed)
- ✅ CORS support for frontend
- ✅ OAuth2/JWT authentication (Keycloak)
- ✅ Request correlation IDs and logging

See detailed documentation: [backend/api-gateway/README.md](backend/api-gateway/README.md)
