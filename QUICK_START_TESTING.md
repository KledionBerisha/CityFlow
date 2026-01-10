# 🚀 CityFlow - Complete System Startup Guide

**For:** Starting and testing the complete CityFlow Real-Time Traffic Monitoring System  
**Time Required:** 10-15 minutes  
**Location:** Prishtina, Kosovo

---

## 📋 Prerequisites

Before starting, ensure you have:

- ✅ Docker Desktop running
- ✅ Node.js installed (for frontend)
- ✅ At least 8GB RAM available for Docker

---

## 🎯 Quick Start (One Command)

**Start everything with a single command:**

```powershell
# Navigate to project root
cd C:\Users\Asus\OneDrive\Desktop\CityFlow

# Start all backend services
docker-compose up -d

# Wait for services to be ready (about 60-90 seconds)
Start-Sleep -Seconds 90

# Check all services are running
docker ps --format "table {{.Names}}\t{{.Status}}" | Select-String "cityflow"
```

**Then start the frontend:**

```powershell
cd frontend
npm run dev
```

**Open browser:** http://localhost:5174

---

## 📖 Step-by-Step Guide

### Step 1: Start Core Infrastructure (2 minutes)

```powershell
cd C:\Users\Asus\OneDrive\Desktop\CityFlow

# Start all services
docker-compose up -d
```

This starts:
| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5433 | Route & analytics data |
| MongoDB | 27017 | Real-time events |
| Redis | 6379 | Caching & rate limiting |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |
| Keycloak | 8080 | Authentication (disabled) |

### Step 2: Wait for Infrastructure (1 minute)

```powershell
# Wait for databases and Kafka to be ready
Start-Sleep -Seconds 60

# Verify infrastructure
docker logs cityflow-kafka-init-1 2>&1 | Select-Object -Last 5
```

**Expected:** "Created topic bus.location.events", etc.

### Step 3: Verify Backend Services (1 minute)

```powershell
# Check which services are running
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**You should see these services running:**

| Service | Port | Status |
|---------|------|--------|
| api-gateway | 8000 | Up |
| route-mgmt-service | 8081 | Up |
| bus-ingestion-service | 8082 | Up |
| traffic-ingestion-service | 8083 | Up |
| analytics-service | 8084 | Up |
| notification-service | 8085 | Up |
| incident-detection-service | 8086 | Up |
| car-ingestion-service | 8088 | Up |

### Step 4: Test API Gateway

```powershell
# Check API Gateway routes
$routes = Invoke-RestMethod http://localhost:8000/actuator/gateway/routes
Write-Host "Registered routes: $($routes.Count)"
$routes | ForEach-Object { Write-Host "  - $($_.route_id): $($_.uri)" }
```

**Expected:** 8 routes registered (bus, traffic, routes, analytics, incidents, notifications, cars, ml)

### Step 5: Start Frontend (1 minute)

```powershell
cd C:\Users\Asus\OneDrive\Desktop\CityFlow\frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

**Expected:** `Local: http://localhost:5174`

### Step 6: Open the Application

Open browser and navigate to: **http://localhost:5174**

---

## 🗺️ Testing the Live Map

### What You Should See:

1. **50 Cars (🚗)** - Moving along real Prishtina streets:
   - Bill Clinton Boulevard
   - Mother Teresa Boulevard
   - Agim Ramadani Street
   - Ring Road M9
   - And more...

2. **4 Buses (🚌)** - Real Trafiku Urban routes:
   - **Line 1** - Qendra → Veternik (every 15 min)
   - **Line 1A** - Qendra → Aeroport Adem Jashari (every 60 min, express)
   - **Line 3** - Qendra → Mati → Germia Park (every 20 min)
   - **Line 4** - Qendra → Arbëria (every 20 min)

3. **15 Traffic Sensors (🟢/🟡)** - At major intersections:
   - Bill Clinton & Mother Teresa Square
   - Grand Hotel Roundabout
   - University of Prishtina
   - Newborn Monument
   - Government Quarter
   - Central Bus Station
   - Ring Road M9
   - Dragodan, Ulpiana, Arbëria, Germia, Mati, Sunny Hill, Veternik

4. **Data Panel** (right side):
   - Current time
   - Vehicle count (AVG/MAX/MIN)
   - Accident count (last 24 hours)
   - Location: Prishtina

### Interactive Features:

- **Click on a car** → Shows vehicle ID, road name, speed, traffic level
- **Click on a bus** → Shows bus ID, route, next stop
- **Click on a sensor** → Shows sensor name, speed, vehicle count, congestion
- **Toggle cars** → Checkbox in top-left to show/hide 50 cars

---

## 🧪 API Testing Commands

### Test Traffic Data

```powershell
# Get current traffic readings (15 Prishtina sensors)
$traffic = Invoke-RestMethod http://localhost:8000/api/traffic/current
Write-Host "Traffic sensors: $($traffic.Count)"
$traffic | Select-Object -First 5 | ForEach-Object { 
    Write-Host "  $($_.sensorCode): Speed $($_.averageSpeed) km/h, $($_.congestionLevel)"
}
```

### Test Bus Locations

```powershell
# Get current bus positions
$buses = Invoke-RestMethod http://localhost:8000/api/bus-locations/current
Write-Host "Buses tracked: $($buses.Count)"
$buses | ForEach-Object { 
    Write-Host "  $($_.vehicleId): ($($_.latitude), $($_.longitude))"
}
```

### Test Car Locations

```powershell
# Get current car positions (50 cars on Prishtina roads)
$cars = Invoke-RestMethod http://localhost:8000/api/car-locations/current
Write-Host "Cars tracked: $($cars.Count)"
$cars | Select-Object -First 5 | ForEach-Object { 
    Write-Host "  $($_.vehicleId): $($_.roadName) - $($_.speedKmh) km/h"
}
```

### Test Incidents

```powershell
# Get recent incidents
$incidents = Invoke-RestMethod "http://localhost:8000/api/incidents/recent?hoursBack=24"
Write-Host "Incidents (24h): $($incidents.Count)"
```

### Test Routes

```powershell
# Get all bus routes
$routes = Invoke-RestMethod http://localhost:8000/api/routes
Write-Host "Routes: $($routes.Count)"
```

---

## 🤖 ML Prediction Service (Optional)

To enable traffic predictions:

### Start ML Services

```powershell
cd C:\Users\Asus\OneDrive\Desktop\CityFlow\data-processing\machine-learning

# Create virtual environment (first time)
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Train models (first time, takes 5-10 minutes)
python train_simple.py

# Start ML API
python model_serving_api.py
```

### Test ML API

```powershell
# Health check
Invoke-RestMethod http://localhost:8090/health

# Predict congestion duration
$body = @{
    road_segment_id = "BILL-CLINTON-BLVD"
    current_speed_kmh = 25
    normal_speed_kmh = 50
    current_congestion_level = 0.6
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8090/predict/congestion-duration" -Method Post -Body $body -ContentType "application/json"
```

**Response:** Estimated minutes until congestion clears

---

## 📊 Service URLs Reference

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:5174 | React web application |
| **API Gateway** | http://localhost:8000 | Main API entry point |
| **Route Management** | http://localhost:8081 | Bus routes & stops |
| **Bus Service** | http://localhost:8082 | Bus tracking |
| **Traffic Service** | http://localhost:8083 | Traffic sensors |
| **Analytics** | http://localhost:8084 | Data analytics |
| **Notifications** | http://localhost:8085 | Alerts |
| **Incidents** | http://localhost:8086 | Incident detection |
| **Car Service** | http://localhost:8088 | Car tracking |
| **ML API** | http://localhost:8090 | Predictions (optional) |
| **Keycloak** | http://localhost:8080 | Auth (disabled) |

---

## 🔄 Managing Services

### Stop All Services

```powershell
cd C:\Users\Asus\OneDrive\Desktop\CityFlow
docker-compose down
```

### Restart a Specific Service

```powershell
docker-compose restart traffic-ingestion-service
```

### View Service Logs

```powershell
# All services
docker-compose logs -f

# Specific service
docker logs cityflow-car-ingestion-service-1 -f
```

### Rebuild After Code Changes

```powershell
docker-compose build <service-name> --no-cache
docker-compose up -d <service-name>
```

### Reset Database (Fresh Start)

```powershell
# Stop services
docker-compose down

# Remove sensor data for fresh Prishtina locations
docker exec cityflow-mongo-1 mongosh cityflow --eval "db.sensors.drop()"

# Restart
docker-compose up -d
```

---

## 🐛 Troubleshooting

### Services Won't Start

```powershell
# Check Docker resources
docker system df

# Restart Docker Desktop, then:
docker-compose up -d
```

### API Returns 404

```powershell
# Check API gateway routes
Invoke-RestMethod http://localhost:8000/actuator/gateway/routes

# If routes missing, rebuild:
docker-compose build api-gateway --no-cache
docker-compose up -d api-gateway
```

### No Cars/Buses on Map

```powershell
# Check service logs
docker logs cityflow-car-ingestion-service-1 2>&1 | Select-Object -Last 20
docker logs cityflow-bus-ingestion-service-1 2>&1 | Select-Object -Last 20
```

### Frontend Won't Load

```powershell
cd frontend
npm install
npm run dev
```

### Port Already in Use

```powershell
# Find what's using the port
netstat -ano | findstr :8000

# Kill the process
taskkill /PID <PID> /F
```

---

## ✅ Success Checklist

```
□ Docker Desktop running
□ docker-compose up -d completes without errors
□ All 8+ containers showing "Up" status
□ API Gateway returns 8 routes
□ Frontend loads at http://localhost:5174
□ Live Map shows:
  □ 50 cars moving on roads
  □ 4 buses on routes
  □ 15 traffic sensors at intersections
  □ Real-time vehicle count updates
□ Click on markers shows popup information
□ Events page loads (may show incidents)
□ Dashboard shows statistics
```

---

## 🎉 You're Ready!

The CityFlow system is now running with:

- **Real-time traffic simulation** for Prishtina, Kosovo
- **50 cars** following actual city roads
- **4 buses** on realistic routes
- **15 sensors** at major intersections
- **Congestion hotspots** during peak hours (7-9am, 4-7pm)
- **Incident detection** for traffic anomalies
- **ML predictions** for congestion duration (optional)

---

## 📸 Screenshots for Documentation

Recommended screenshots:
1. **Live Map** - Full view with cars, buses, and sensors
2. **Car Popup** - Showing road name and speed
3. **Traffic Sensor Popup** - Showing congestion level
4. **Events Page** - Detected incidents
5. **Dashboard** - Statistics overview
6. **Docker containers** - Running services

---

**Last Updated:** January 2026  
**System:** CityFlow Real-Time Traffic Monitoring  
**Location:** Prishtina, Kosovo
