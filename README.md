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
   - Kafka brokers: `localhost:9093`
   - Postgres: `localhost:5433` (user/pass: `kledionberisha` / `kledion123`, db: `cityflow`)
   - MongoDB: `localhost:27017`
   - Keycloak: `http://localhost:8080` (admin/admin)
   - Kafka UI: `http://localhost:8081`

## Route Management Service (current status)
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
- Schema (Flyway-managed):
  - routes, stops, route_stops, schedules, buses, users
  - Route fields map to DB columns: `route_code`, `route_name`, `is_active`
