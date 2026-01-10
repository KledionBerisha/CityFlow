# 🧪 CityFlow - Manual Testing Steps (Windows)

**Status:** Schema Registry Running | Schemas Pending Registration

---

## ✅ **Completed Steps**

### 1. Schema Registry Infrastructure - RUNNING ✅

All services are up and healthy:

```
✓ Zookeeper: Running (port 2182)
✓ Kafka: Running (ports 9094, 29094)  
✓ Schema Registry API: Running (port 8091)
✓ Schema Registry UI: http://localhost:8092
✓ Kafka Topics UI: http://localhost:8093
✓ REST Proxy: http://localhost:8094
```

**Verification:**
```powershell
docker-compose ps  # All services should show "Up" and "healthy"
Invoke-WebRequest -Uri "http://localhost:8091/" -UseBasicParsing  # Should return version info
```

---

## 📝 **Next Steps - Manual Schema Registration**

### Option 1: Register Schemas via REST API (Recommended for Windows)

**1. Register Traffic Reading Event:**
```powershell
cd data-processing\schemas

$schema = Get-Content .\avro\traffic-reading-event.avsc -Raw | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100

$body = @{
    schema = $schema
    schemaType = "AVRO"
} | ConvertTo-Json -Depth 100

Invoke-RestMethod -Uri "http://localhost:8091/subjects/traffic.reading.events-value/versions" `
    -Method Post `
    -ContentType "application/vnd.schemaregistry.v1+json" `
    -Body $body
```

**2. Register Bus Location Event:**
```powershell
$schema = Get-Content .\avro\bus-location-event.avsc -Raw | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100

$body = @{
    schema = $schema
    schemaType = "AVRO"
} | ConvertTo-Json -Depth 100

Invoke-RestMethod -Uri "http://localhost:8091/subjects/bus.location.events-value/versions" `
    -Method Post `
    -ContentType "application/vnd.schemaregistry.v1+json" `
    -Body $body
```

**3. Register Incident Event:**
```powershell
$schema = Get-Content .\avro\incident-event.avsc -Raw | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100

$body = @{
    schema = $schema
    schemaType = "AVRO"
} | ConvertTo-Json -Depth 100

Invoke-RestMethod -Uri "http://localhost:8091/subjects/incident.events-value/versions" `
    -Method Post `
    -ContentType "application/vnd.schemaregistry.v1+json" `
    -Body $body
```

**4. Verify Registration:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8091/subjects" -Method Get
# Should return: ["traffic.reading.events-value","bus.location.events-value","incident.events-value"]
```

### Option 2: Use Schema Registry UI

1. Open http://localhost:8092 in your browser
2. Click "NEW" button
3. Paste schema content from `.avsc` files
4. Set subject name (e.g., `traffic.reading.events-value`)
5. Click "Register"
6. Repeat for all 5 schemas

---

## 🔥 **After Schemas Are Registered - Continue with Spark**

### 1. Build Spark Jobs

```powershell
cd ..\spark-streaming

# Clean and compile
mvn clean compile

# Package JAR
mvn package

# Verify
dir target\spark-streaming-jobs-1.0.0.jar
```

### 2. Update Spark Configuration

Since backend is using existing Kafka on port 9093, update `src\main\resources\application.conf`:

```hocon
cityflow.kafka {
  bootstrap-servers = "localhost:9093"  # Use existing Kafka
  schema-registry-url = "http://localhost:8091"  # Our new Schema Registry
}
```

### 3. Start Spark Cluster

Since port 8080 is taken by Keycloak, we need to update Spark's `docker-compose.yml`:

```yaml
environment:
  - SPARK_MASTER_WEBUI_PORT=9080  # Change from 8080 to 9080
ports:
  - "9080:9080"  # Change from 8080:8080
```

Then start:
```powershell
docker-compose up -d
```

**Access Spark UI at:** http://localhost:9080

### 4. Generate Test Data

Since backend is running, use existing backend services to generate events:

```powershell
# Traffic Ingestion Service is already running and generating data!
# Check it's producing to Kafka:
docker exec cityflow-kafka-1 kafka-console-consumer `
  --bootstrap-server localhost:9093 `
  --topic traffic.reading.events `
  --from-beginning `
  --max-messages 5
```

### 5. Submit Spark Jobs

```powershell
# Option A: Submit all jobs
.\submit-jobs.ps1

# Option B: Submit individual jobs
.\submit-jobs.ps1 data-lake
.\submit-jobs.ps1 traffic
.\submit-jobs.ps1 analytics
```

### 6. Monitor Execution

**Spark Master UI:** http://localhost:9080
**Job UI:** http://localhost:4040 (when job is running)

### 7. Verify Outputs

**Delta Lake:**
```powershell
dir C:\tmp\cityflow\delta-lake -Recurse
```

**PostgreSQL:**
```powershell
docker exec -it cityflow-postgres-1 psql -U postgres -d cityflow
# Then: SELECT * FROM traffic_aggregated_30min LIMIT 10;
```

**MongoDB:**
```powershell
docker exec -it cityflow-mongo-1 mongosh cityflow
# Then: db.bus_positions.find().limit(5)
```

**Redis:**
```powershell
docker exec -it cityflow-redis-1 redis-cli
# Then: GET cityflow:analytics:realtime
```

---

## 🐛 **Common Issues & Solutions**

### Issue: Schema Registration Fails
**Solution:** Use Schema Registry UI (http://localhost:8092) for manual registration

### Issue: Spark Can't Connect to Kafka
**Solution:** 
- Check backend Kafka is running: `docker ps | grep kafka`
- Use correct bootstrap servers in application.conf: `localhost:9093`

### Issue: Port Conflicts
**Solution:** 
- Backend uses: 8080-8086, 9092-9093
- Schema Registry uses: 8091-8094, 9094, 2182
- Spark uses: 9080, 4040+

### Issue: Out of Memory
**Solution:** 
- Increase Docker memory to 8GB
- Reduce Spark executor memory in submit scripts

---

## ✅ **Success Criteria**

Your testing is successful when:

- [ ] All Docker containers running without errors
- [ ] At least 3 schemas registered in Schema Registry
- [ ] Spark cluster shows 1 master + 2 workers
- [ ] At least 1 Spark job running
- [ ] Data appearing in Delta Lake directory
- [ ] No exceptions in Spark logs

---

## 📊 **Current Status**

```
Phase 1: Schema Registry       ████████████  100%  ✅
Phase 2: Schema Registration   ████████░░░░   60%  ⏸️
Phase 3: Spark Build          ░░░░░░░░░░░░    0%  ⏳
Phase 4: Spark Deployment     ░░░░░░░░░░░░    0%  ⏳
Phase 5: Job Submission       ░░░░░░░░░░░░    0%  ⏳
Phase 6: Verification         ░░░░░░░░░░░░    0%  ⏳
```

---

## 📚 **Quick Reference**

**Schema Registry:**
- API: http://localhost:8091
- UI: http://localhost:8092
- Topics UI: http://localhost:8093

**Spark (after start):**
- Master UI: http://localhost:9080
- Job UI: http://localhost:4040

**Backend Services (already running):**
- Keycloak: http://localhost:8080
- API Gateway: http://localhost:8000
- Traffic Service: http://localhost:8083

**Databases:**
- PostgreSQL: localhost:5433
- MongoDB: localhost:27017
- Redis: localhost:6379

---

**Next Action:** Register schemas (Option 1 or 2 above), then proceed with Spark build and deployment.
