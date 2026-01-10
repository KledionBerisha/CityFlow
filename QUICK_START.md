# CityFlow - Quick Start Guide

## 🚀 Complete Startup Sequence

### 1. Start All Services (Required)

```powershell
# Navigate to project root
cd C:\Users\Asus\OneDrive\Desktop\CityFlow

# Start all Docker services
docker-compose up -d

# Wait 30-60 seconds for services to initialize
Start-Sleep -Seconds 45
```

**Services Started:**
- ✅ Zookeeper (Kafka coordination)
- ✅ Kafka (Event streaming)
- ✅ Redis (Caching)
- ✅ PostgreSQL (Route data)
- ✅ MongoDB (Traffic/bus/car readings)
- ✅ Keycloak (Auth - can be disabled)
- ✅ All 7 Backend Microservices
- ✅ API Gateway (Port 8000)
- ✅ MLflow (Port 5001)
- ✅ ML API (Port 8090)

### 2. Train ML Models (REQUIRED for Predictions)

```powershell
# Navigate to ML directory
cd data-processing\machine-learning

# Install Python dependencies (if needed)
pip install xgboost scikit-learn pandas numpy joblib pyyaml

# Train models (takes ~2 minutes)
python train_simple.py
```

**Expected Output:**
```
Models saved:
  - models/traffic_pred_10min.pkl (~400 KB)
  - models/traffic_pred_20min.pkl (~390 KB)
  - models/traffic_pred_30min.pkl (~406 KB)
```

### 3. Restart ML API to Load Models

```powershell
# Go back to project root
cd ..\..\

# Restart ML API to load newly trained models
docker-compose restart ml-api

# Wait 10 seconds
Start-Sleep -Seconds 10

# Verify models loaded
Invoke-WebRequest -Uri "http://localhost:8090/health" -UseBasicParsing | ConvertFrom-Json
```

**Expected Response:**
```json
{
  "status": "healthy",
  "models_loaded": 3,
  "timestamp": "2026-01-09T..."
}
```

### 4. Start Frontend (Optional - for UI)

```powershell
# Navigate to frontend
cd frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev
```

Frontend will be at: **http://localhost:5173**

---

## ✅ Verification Checklist

Run these commands to verify everything is working:

```powershell
# 1. Check all services are running
docker-compose ps
# All should show "Up" status

# 2. Check API Gateway
Invoke-WebRequest -Uri "http://localhost:8000/api/health" -UseBasicParsing
# Should return 200 OK

# 3. Check ML API Health
Invoke-WebRequest -Uri "http://localhost:8090/health" -UseBasicParsing | ConvertFrom-Json
# Should show: "status": "healthy", "models_loaded": 3

# 4. Test ML Predictions
Invoke-WebRequest -Uri "http://localhost:8090/predict/congestion-hotspots?horizon=30" -UseBasicParsing | ConvertFrom-Json
# Should return array of congestion hotspots (may be empty if no traffic data yet)

# 5. Check MLflow (optional)
Invoke-WebRequest -Uri "http://localhost:5001/health" -UseBasicParsing
# Should return 200 OK
```

---

## 🔧 What Each Service Does

### Infrastructure
- **Zookeeper + Kafka**: Event streaming between services
- **Redis**: Fast caching for current readings
- **PostgreSQL**: Stores routes, road segments, analytics
- **MongoDB**: Stores real-time traffic readings, bus/car locations

### Backend Services
- **route-mgmt-service**: Manage routes and road segments
- **bus-ingestion-service**: Track bus locations in real-time
- **traffic-ingestion-service**: Collect traffic sensor data
- **car-ingestion-service**: Track car locations
- **analytics-service**: Aggregate city-wide metrics
- **incident-detection-service**: Detect traffic incidents
- **notification-service**: Send notifications

### API & ML
- **api-gateway**: Single entry point, routes all requests
- **mlflow**: Model tracking and versioning (optional)
- **ml-api**: ML predictions for congestion and traffic

### Frontend
- **React App**: Web UI showing live map, predictions, analytics

---

## 🎯 What You Can Do Now

1. **View Live Map** (`http://localhost:5173` → Live Map)
   - See real-time bus locations
   - See traffic sensor readings
   - See car locations
   - **Toggle "Show Predictions"** to see congestion predictions

2. **View Predictions** (`http://localhost:5173` → Predictions)
   - See biggest congestion hotspots
   - See predicted speeds (10, 20, 30 min ahead)
   - See congestion duration predictions

3. **View Analytics** (`http://localhost:5173` → Analytics)
   - City-wide metrics
   - Traffic patterns
   - Vehicle counts

4. **View Events** (`http://localhost:5173` → Events)
   - Recent incidents
   - Traffic events

---

## 🐛 Common Issues

### "No models loaded"
**Solution:** Train models first:
```powershell
cd data-processing\machine-learning
python train_simple.py
cd ..\..\
docker-compose restart ml-api
```

### "Empty predictions"
**Solution:** This is normal if there's no traffic data. The system will use synthetic data for training but predictions need current traffic readings. Wait for traffic service to generate data, or the ML service will use synthetic data as fallback.

### "Service not responding"
**Solution:** 
1. Check if service is running: `docker-compose ps`
2. Check service logs: `docker-compose logs [service-name]`
3. Restart service: `docker-compose restart [service-name]`

### "Port already in use"
**Solution:** 
1. Find what's using the port: `netstat -ano | findstr :PORT`
2. Stop the conflicting service
3. Or change port in `docker-compose.yml`

---

## 📊 Service URLs

- **Frontend**: http://localhost:5173
- **API Gateway**: http://localhost:8000
- **ML API**: http://localhost:8090
- **MLflow UI**: http://localhost:5001
- **PostgreSQL**: localhost:5433
- **MongoDB**: localhost:27017
- **Redis**: localhost:6379
- **Kafka**: localhost:9093

---

## 🔄 Daily Startup

Just run these 3 commands:

```powershell
docker-compose up -d
Start-Sleep -Seconds 45
cd frontend; npm run dev
```

---

## 📝 Full Documentation

For detailed information, see: **CITYFLOW_STARTUP_GUIDE.md**
