# 🔍 Map Empty - Root Cause Analysis

**Date:** January 10, 2026  
**Issue:** Frontend map shows no data (buses, traffic sensors, road overlays)

---

## ✅ What's Working

1. ✅ **Backend Services:** All Docker containers running
2. ✅ **API Gateway:** Accessible at http://localhost:8000
3. ✅ **Bus Data:** API returns 4 active buses with GPS coordinates
4. ✅ **Frontend:** React app running on port 5174
5. ✅ **Map Component:** Leaflet/React-Leaflet properly configured

---

## ❌ Root Causes

### **CRITICAL #1: Vite Proxy Not Working**

**Problem:**
- Vite config specifies port 3000
- Frontend actually running on port 5174
- **API proxy is not active** - all `/api/*` calls fail

**Evidence:**
```typescript
// vite.config.ts - Line 8
server: {
  port: 3000,  // ❌ But terminal shows: "Local: http://localhost:5174/"
  proxy: { ... }
}
```

**Impact:** Frontend cannot reach backend APIs

---

### **#2: Traffic Readings API Crashes (HTTP 500)**

**Endpoint:** `GET /api/traffic/readings/current`

**Test Result:**
```bash
$ Invoke-WebRequest -Uri "http://localhost:8000/api/traffic/readings/current"
{"error":"Internal Server Error","status":500}
```

**Impact:** No traffic sensors show on map even if proxy worked

---

### **#3: Missing API Endpoints**

**Not Implemented:**
- `/api/incidents` → 404 Not Found
- `/api/map/roads` → Not tested (likely missing)
- `/api/traffic/roads` → Not tested (likely missing)

**Impact:** No incident markers, no road overlays with congestion colors

---

### **#4: Empty Routes Database**

**Endpoint:** `GET /api/routes`

**Response:**
```json
{
  "content": [],
  "totalElements": 0,
  "empty": true
}
```

**Impact:** Buses don't have route assignments (routeId is null)

---

## 🎯 Expected vs Actual

### **Bus Locations** (Working ✅)
```json
// API returns 4 buses with coordinates
[
  {
    "vehicleId": "BUS-001",
    "latitude": 41.646,
    "longitude": 21.597,
    "speedKmh": 35.35,
    "routeId": null  // ⚠️ No route assigned
  }
]
```

### **Traffic Readings** (Broken ❌)
- **Expected:** Array of sensor readings with lat/lng
- **Actual:** HTTP 500 error

### **Routes** (Empty ⚠️)
- **Expected:** List of bus routes (Route 1, Route 2, etc.)
- **Actual:** Empty array

---

## 🔧 Quick Fixes

### Fix #1: Restart Frontend on Correct Port

**Option A - Force Port 3000:**
```bash
# Kill process on 5173/5174
# Then run:
cd frontend
npm run dev
# Should now run on 3000 with working proxy
```

**Option B - Update Vite Config:**
```typescript
// vite.config.ts
server: {
  port: 5174,  // Match actual port
  proxy: {
    '/api': {
      target: 'http://localhost:8000',
      changeOrigin: true,
    },
  },
}
```

---

### Fix #2: Debug Traffic Service

Check `traffic-ingestion-service` logs:
```bash
docker logs cityflow-traffic-ingestion-service-1
```

Likely issues:
- MongoDB connection error
- Redis cache issue
- Null pointer when reading current traffic

---

### Fix #3: Add Missing API Gateway Routes

File: `backend/api-gateway/src/main/java/.../GatewayConfig.java`

Add missing routes:
- `/api/incidents` → `incident-detection-service:8086`
- `/api/map/roads` → (needs new endpoint)
- `/api/traffic/roads` → `traffic-ingestion-service:8083`

---

### Fix #4: Create Sample Routes

```bash
# Run the setup-route.ps1 script
.\setup-route.ps1
```

---

## 🧪 Verification Steps

After fixes, test:

1. **Proxy Working:**
```bash
# From frontend (localhost:3000), open browser console:
fetch('/api/bus-locations/current').then(r => r.json()).then(console.log)
# Should return bus array, not CORS/404 error
```

2. **Traffic API Fixed:**
```bash
Invoke-WebRequest -Uri "http://localhost:8000/api/traffic/readings/current"
# Should return 200 OK with sensor array
```

3. **Map Shows Data:**
- Open http://localhost:3000/live-map
- Should see: 4 blue bus markers
- Should see: Traffic sensor circles (if API fixed)

---

## 📊 Summary

| Component | Status | Impact on Map |
|-----------|--------|---------------|
| Vite Proxy | ❌ Broken | No API access → Empty map |
| Bus API | ✅ Working | Buses should show (if proxy works) |
| Traffic API | ❌ Crashes | No sensors show |
| Incidents API | ❌ Missing | No incident markers |
| Routes API | ⚠️ Empty | Buses have no route info |
| Map Component | ✅ Works | Renders correctly when data available |

---

## 🎬 Next Steps

**Priority Order:**

1. **FIX PROXY** (highest priority)
   - Restart frontend on port 3000 or update config

2. **FIX TRAFFIC API** (high priority)
   - Check service logs
   - Debug /traffic/readings/current endpoint

3. **CREATE ROUTES** (medium)
   - Run setup-route.ps1

4. **ADD MISSING ENDPOINTS** (medium)
   - Implement /api/incidents
   - Implement /api/traffic/roads or /api/map/roads

---

**Expected Result After Fixes:**
- Map shows 4 moving buses (BUS-001 to BUS-005)
- Map shows traffic sensors (once API fixed)
- Data panels show real counts
- No console errors
