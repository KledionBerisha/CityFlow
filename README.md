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
   - Postgres: `localhost:5432` (user/pass/db: `cityflow`)
   - MongoDB: `localhost:27017`
   - Keycloak: `http://localhost:8080` (admin/admin)
   - Kafka UI: `http://localhost:8081`