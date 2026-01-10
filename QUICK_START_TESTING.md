# 🚀 CityFlow - Quick Start Testing Guide

**For:** Testing the complete CityFlow system with ML predictions  
**Time Required:** 15-20 minutes  
**Difficulty:** Easy

---

## 📋 Prerequisites

✅ Docker Desktop running  
✅ Python 3.12 installed  
✅ Node.js installed  
✅ All services built (done today!)

---

## 🎯 Quick Test - Frontend Only

**If you just want to see the frontend with ML predictions:**

### Step 1: Start ML Services (5 minutes)

```powershell
cd C:\Users\Asus\OneDrive\Desktop\CityFlow\data-processing\machine-learning

# Start MLflow and ML API
docker-compose up -d

# Wait for services to start
Start-Sleep -Seconds 30

# Check ML API health
Invoke-RestMethod http://localhost:8090/health
```

**Expected Output:** `{"status": "healthy", "timestamp": "..."}`

### Step 2: Start Frontend (2 minutes)

```powershell
cd C:\Users\Asus\OneDrive\Desktop\CityFlow\frontend

# Start development server
npm run dev
```

**Expected Output:** `Local: http://localhost:3000`

### Step 3: View Predictions Page (1 minute)

1. Open browser: **http://localhost:3000**
2. Click **"Predict"** in the sidebar
3. You should see:
   - ✅ Green "ML Prediction Service Active" banner
   - Prediction cards with forecasts
   - Auto-refresh every 30 seconds

**Note:** If no predictions show, it's because no traffic data exists yet. This is expected for first run.

---

## 🔧 Full Integration Test

**For complete end-to-end testing:**

### Step 1: Start All Infrastructure (10 minutes)

```powershell
# Terminal 1: Core services
cd C:\Users\Asus\OneDrive\Desktop\CityFlow
docker-compose up -d

# Terminal 2: Schema Registry
cd data-processing\schemas
docker-compose up -d

# Wait for everything to start
Start-Sleep -Seconds 60
```

### Step 2: Verify Services

```powershell
# Check what's running
docker ps

# You should see:
# - kafka
# - zookeeper  
# - schema-registry
# - postgres
# - mongodb
# - redis
```

### Step 3: Start Spark Streaming (Optional)

```powershell
cd data-processing\spark-streaming

# Start Spark cluster
docker-compose up -d

# Wait for Spark to be ready
Start-Sleep -Seconds 30

# Submit streaming jobs
.\submit-jobs.ps1
```

### Step 4: Train ML Models (First Time Only)

```powershell
cd data-processing\machine-learning

# Generate synthetic data and train models (5-10 minutes)
python train_models.py
```

**Expected Output:**
```
Training model for 10-minute predictions...
Training model for 20-minute predictions...
Training model for 30-minute predictions...
✓ Models saved to models/
✓ Logged to MLflow
```

### Step 5: Start ML Services

```powershell
# Still in data-processing/machine-learning
docker-compose up -d
```

### Step 6: Start Frontend

```powershell
cd ..\..\frontend
npm run dev
```

### Step 7: Test Everything

**Frontend:** http://localhost:3000
- ✅ Dashboard loads
- ✅ Live Map shows (may be empty)
- ✅ Predict page shows ML forecasts
- ✅ Status indicators show green

**MLflow:** http://localhost:5001
- ✅ Experiments visible
- ✅ Model metrics logged
- ✅ Artifacts saved

**Schema Registry:** http://localhost:8092
- ✅ 5 schemas registered
- ✅ UI accessible

---

## 🧪 Manual API Tests

### Test ML API

```powershell
# Health check
Invoke-RestMethod http://localhost:8090/health

# Get all predictions
Invoke-RestMethod http://localhost:8090/predict/all?horizon=30

# Custom prediction
$body = @{
    readings = @(@{
        road_segment_id = "SEGMENT_001"
        timestamp = (Get-Date).ToString("o")
        speed_kmh = 45.5
        vehicle_count = 120
    })
    prediction_horizons = @(10, 20, 30)
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri http://localhost:8090/predict -Method Post -Body $body -ContentType "application/json"
```

### Test Backend API (if running)

```powershell
# Bus locations
Invoke-RestMethod http://localhost:8000/api/bus-locations/current

# Routes
Invoke-RestMethod http://localhost:8000/api/routes

# Incidents
Invoke-RestMethod http://localhost:8000/api/incidents
```

---

## 🎨 Frontend Features to Test

### 1. Predict Page
- [x] ML service status indicator
- [x] Prediction cards grid
- [x] Trend indicators (↑ ↓ →)
- [x] Congestion level badges
- [x] Multiple horizons (10/20/30 min)
- [x] Percentage changes
- [x] Refresh button
- [x] Auto-refresh (30s)
- [x] Loading states
- [x] Error handling

### 2. Live Map (if backend running)
- [x] Bus markers
- [x] Prediction markers (purple)
- [x] Traffic overlays
- [x] Interactive popups
- [x] Real-time updates

### 3. Dashboard
- [x] Stats cards
- [x] Vehicle count
- [x] Accident count
- [x] Location display

---

## 🐛 Troubleshooting

### ML API Shows "Unavailable"

**Cause:** Docker services not running

**Fix:**
```powershell
cd data-processing\machine-learning
docker-compose up -d
Start-Sleep -Seconds 30
```

### No Predictions Showing

**Cause:** Models not trained or no data

**Fix:**
```powershell
cd data-processing\machine-learning
python train_models.py
```

### Frontend Won't Start

**Cause:** Dependencies not installed

**Fix:**
```powershell
cd frontend
npm install
npm run dev
```

### Port Already in Use

**Cause:** Service already running

**Fix:**
```powershell
# Find what's using the port
netstat -ano | findstr :8090

# Kill the process (replace PID)
taskkill /PID <PID> /F

# Or restart Docker
docker-compose restart
```

---

## 📊 Expected Results

### ✅ Success Indicators

**Frontend:**
- Loads without errors
- Predict page shows green status
- Cards display with data
- No console errors

**ML API:**
- Health check returns 200
- Predictions return valid JSON
- Response time < 1 second

**MLflow:**
- UI loads
- Experiments visible
- Models logged

**Services:**
- All Docker containers running
- No restart loops
- Logs show no errors

---

## 🎯 Quick Validation Checklist

```
□ Docker Desktop running
□ Schema Registry accessible (http://localhost:8092)
□ 5 schemas registered
□ ML API health check passes (http://localhost:8090/health)
□ MLflow UI loads (http://localhost:5001)
□ Frontend dev server started (http://localhost:3000)
□ Predict page shows green "ML Service Active"
□ Can see prediction cards (or "No data" message)
□ Refresh button works
□ No browser console errors
```

---

## 🎉 You're Done!

If all checks pass, your CityFlow system is ready for:
- ✅ Demo/presentation
- ✅ Academic evaluation
- ✅ Further development
- ✅ Documentation screenshots

---

## 📸 Screenshots for Report

**Recommended Screenshots:**

1. **Frontend - Predict Page** - ML predictions grid
2. **Frontend - Live Map** - Bus tracking
3. **MLflow UI** - Experiment tracking
4. **Schema Registry UI** - Registered schemas
5. **Kafka Topics UI** - Topic browser
6. **Architecture Diagram** - System overview

---

## 🔗 Service URLs Reference

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | None |
| API Gateway | http://localhost:8000 | None |
| ML API | http://localhost:8090 | None |
| MLflow | http://localhost:5001 | None |
| Schema Registry | http://localhost:8091 | None |
| Schema Registry UI | http://localhost:8092 | None |
| Kafka Topics UI | http://localhost:8093 | None |
| PostgreSQL | localhost:5433 | kledionberisha/kledion123 |
| MongoDB | localhost:27017 | None |
| Redis | localhost:6379 | None |

---

## 💾 Data Persistence

**Volumes:**
- PostgreSQL: `cityflow-postgres-data`
- MongoDB: `cityflow-mongo-data`
- Redis: `cityflow-redis-data`
- Kafka: `kafka-data`
- Zookeeper: `zookeeper-data`
- MLflow: `mlflow-data`

**To reset everything:**
```powershell
docker-compose down -v
# This deletes all data!
```

---

**Status:** Ready for testing!  
**Estimated Time:** 15-20 minutes for full test  
**Difficulty:** Easy  
**Support:** See troubleshooting section above
