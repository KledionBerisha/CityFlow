# 🧪 CityFlow Data Processing - Complete Testing Guide

**Steps 1 + 2 Integration Testing**

This guide provides comprehensive instructions for testing the complete data processing pipeline: **Schema Registry** (Step 1) + **Spark Streaming Jobs** (Step 2).

---

## 📋 Prerequisites Checklist

Before starting, ensure you have:

- [x] **Java 17** installed (`java -version`)
- [x] **Maven 3.8+** installed (`mvn -version`)
- [x] **Docker & Docker Compose** installed (`docker --version`)
- [x] **jq** installed for JSON processing (`jq --version`)
- [x] **curl** for API testing
- [x] **Python 3.8+** (optional, for test data generation)
- [x] At least **8GB RAM** available for Docker
- [x] At least **10GB** free disk space

---

## 🚀 Phase 1: Start Infrastructure

### Step 1.1: Start Schema Registry & Kafka

```bash
# Navigate to schemas directory
cd data-processing/schemas

# Start Schema Registry infrastructure
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
echo "Waiting for services to start..."
sleep 60

# Verify all services are running
docker-compose ps

# Expected output: All services should be "Up" and "healthy"
```

**Verify Schema Registry:**
```bash
curl http://localhost:8081/
# Expected: {"version":"7.5.0","commit":"..."}
```

**Verify Web UIs:**
- Schema Registry UI: http://localhost:8082
- Kafka Topics UI: http://localhost:8083

### Step 1.2: Validate and Register Schemas

```bash
cd registry

# Validate all Avro schemas
chmod +x validate-schemas.sh
./validate-schemas.sh

# Expected: All 5 schemas should validate successfully

# Register schemas with Schema Registry
chmod +x register-schemas.sh
./register-schemas.sh

# Expected: All 5 schemas registered with IDs 1-5
```

**Verify Registration:**
```bash
curl http://localhost:8081/subjects
# Expected: ["traffic.reading.events-value","bus.location.events-value",...]

curl http://localhost:8081/subjects/traffic.reading.events-value/versions/latest | jq
# Expected: Full schema definition
```

✅ **Phase 1 Complete:** Schema Registry is running with all schemas registered!

---

## 🔥 Phase 2: Build Spark Jobs

### Step 2.1: Generate Avro Classes and Build JAR

```bash
# Navigate to Spark Streaming directory
cd ../../spark-streaming

# Clean previous builds
mvn clean

# Compile and generate Avro classes from schemas
mvn compile

# Verify Avro classes were generated
ls -la target/generated-sources/avro/com/cityflow/events/

# Expected: Avro classes for all 5 event types

# Package JAR with all dependencies
mvn package

# Verify JAR creation
ls -lh target/spark-streaming-jobs-1.0.0.jar

# Expected: JAR file ~50-100MB in size
```

✅ **Phase 2 Complete:** Spark jobs compiled and packaged!

---

## ⚡ Phase 3: Start Spark Cluster

### Step 3.1: Launch Spark Master and Workers

```bash
# Start Spark cluster (master + 2 workers)
docker-compose up -d

# Wait for Spark to initialize (20-30 seconds)
sleep 30

# Verify all Spark services are running
docker-compose ps

# Expected: spark-master, spark-worker-1, spark-worker-2 all "Up"
```

### Step 3.2: Verify Spark Cluster

**Check Spark Master UI:**
```bash
# Open in browser
open http://localhost:8080  # Mac
start http://localhost:8080  # Windows
```

**Expected:**
- Status: ALIVE
- Workers: 2
- Cores: 4 total
- Memory: 4.0 GB total

✅ **Phase 3 Complete:** Spark cluster is running!

---

## 📊 Phase 4: Generate Test Data

### Option A: Using Python Examples (Recommended)

```bash
# Navigate to schema examples
cd ../schemas/examples/python

# Install dependencies (if not already installed)
pip install -r requirements.txt

# Start producer in background
python traffic_reading_producer.py &
PRODUCER_PID=$!

# Let it run for 30 seconds to generate test data
sleep 30

# Stop producer
kill $PRODUCER_PID

echo "Test data generation complete!"
```

### Option B: Using Java Examples

```bash
cd ../java

# Compile
mvn compile

# Run producer
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingProducer"
```

### Option C: Using Backend Services

If you have backend services running:
```bash
# Start traffic simulator from backend
cd ../../../../backend/traffic-ingestion-service
mvn spring-boot:run
```

**Verify Data in Kafka:**
```bash
# Check messages in topic
docker exec cityflow-kafka-schema kafka-console-consumer \
  --bootstrap-server localhost:9094 \
  --topic traffic.reading.events \
  --from-beginning \
  --max-messages 5
```

✅ **Phase 4 Complete:** Test data is flowing to Kafka!

---

## 🎯 Phase 5: Submit Spark Jobs

### Step 5.1: Submit All Jobs

```bash
# Navigate back to spark-streaming
cd ../../../../spark-streaming

# Make scripts executable
chmod +x submit-jobs.sh

# Submit all jobs
./submit-jobs.sh

# Expected output:
# [SUCCESS] CityFlow-DataLakeIngestion submitted (PID: xxxx)
# [SUCCESS] CityFlow-TrafficAggregation submitted (PID: xxxx)
# [SUCCESS] CityFlow-BusETL submitted (PID: xxxx)
# [SUCCESS] CityFlow-RealTimeAnalytics submitted (PID: xxxx)
```

### Step 5.2: Verify Jobs Are Running

**Check running processes:**
```bash
./submit-jobs.sh --list
```

**Check Spark UI:**
- Master UI: http://localhost:8080
- Look for "Running Applications" section
- Should see 4 applications running

**Check individual job UIs:**
- Each job has its own UI (ports 4040, 4041, 4042, 4043)

✅ **Phase 5 Complete:** All Spark jobs are running!

---

## 🔍 Phase 6: Monitor and Verify Outputs

### 6.1: Monitor Spark Execution

**Real-time Monitoring:**
```bash
# Watch Spark Master UI
open http://localhost:8080

# Watch first application details
open http://localhost:4040
```

**Check for:**
- ✅ Active Queries (Streaming tab)
- ✅ Processing time per batch
- ✅ Input rate (records/second)
- ✅ No failed tasks

### 6.2: Verify Delta Lake Output

```bash
# Check Delta Lake directory structure
ls -R /tmp/cityflow/delta-lake/

# Expected structure:
# /tmp/cityflow/delta-lake/
# ├── traffic/
# │   ├── raw/
# │   ├── aggregated_5min/
# │   ├── aggregated_15min/
# │   └── aggregated_30min/
# ├── bus/
# │   └── raw/
# └── incident/
#     └── raw/

# Check if data is being written
watch -n 5 'ls -lh /tmp/cityflow/delta-lake/traffic/raw/date=*/hour=*/'
```

**Read Delta Lake data (requires spark-shell):**
```bash
spark-shell

scala> val df = spark.read.format("delta").load("/tmp/cityflow/delta-lake/traffic/raw")
scala> df.count()  // Should show number of records
scala> df.show(5)  // Show sample data
scala> df.printSchema()
```

### 6.3: Verify PostgreSQL Output

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d cityflow

# Check if table exists
\dt traffic_aggregated_30min

# Query aggregated data
SELECT 
    window_start, 
    road_segment_id, 
    avg_speed, 
    total_vehicles,
    reading_count
FROM traffic_aggregated_30min 
ORDER BY window_start DESC 
LIMIT 10;

# Expected: Aggregated traffic data per road segment
```

### 6.4: Verify MongoDB Output

```bash
# Connect to MongoDB
mongosh mongodb://localhost:27017/cityflow

# Check bus positions collection
db.bus_positions.countDocuments()
# Expected: Number of processed bus events

# Query sample data
db.bus_positions.find().limit(5).pretty()

# Expected: Bus location data with geospatial calculations
```

### 6.5: Verify Redis Cache

```bash
# Connect to Redis
redis-cli

# Check analytics data
GET cityflow:analytics:realtime

# Expected: JSON object with real-time KPIs
# Example: {"city_avg_speed": 45.2, "total_vehicles": 1234, ...}

# Check TTL
TTL cityflow:analytics:realtime
# Expected: ~60 seconds
```

✅ **Phase 6 Complete:** All outputs verified!

---

## 📈 Phase 7: Performance Validation

### 7.1: Check Processing Metrics

**Spark Streaming UI (http://localhost:4040):**
1. Go to "Streaming" tab
2. Check:
   - **Input Rate:** Should be consistent
   - **Processing Time:** Should be < trigger interval
   - **Scheduling Delay:** Should be minimal (< 1s)
   - **Batch Duration:** Should be stable

**Expected Metrics:**
- Input Rate: 10-100 records/sec (depending on data volume)
- Processing Time: < 30 seconds
- Total Delay: < 1 second

### 7.2: Check Resource Usage

```bash
# Docker stats
docker stats

# Expected:
# - Spark workers: 40-60% CPU, 1-2GB RAM
# - Kafka: 10-20% CPU, 512MB RAM
# - Schema Registry: 5-10% CPU, 256MB RAM
```

### 7.3: Check Data Quality

**Validate no data loss:**
```bash
# Count input messages
docker exec cityflow-kafka-schema kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9094 \
  --topic traffic.reading.events

# Compare with Delta Lake count
spark-shell
scala> spark.read.format("delta").load("/tmp/cityflow/delta-lake/traffic/raw").count()

# Numbers should match (within watermark tolerance)
```

✅ **Phase 7 Complete:** Performance validated!

---

## 🎬 Phase 8: End-to-End Test Scenario

### Complete Flow Test:

1. **Generate burst of traffic data**
```bash
cd ../schemas/examples/python
python traffic_reading_producer.py &
sleep 60
```

2. **Wait for processing**
```bash
# Wait 2-3 minutes for windowing and aggregation
sleep 180
```

3. **Query results across all sinks**

**Delta Lake:**
```bash
spark-shell
scala> val traffic = spark.read.format("delta").load("/tmp/cityflow/delta-lake/traffic/aggregated_30min")
scala> traffic.orderBy($"window_start".desc).show(5)
```

**PostgreSQL:**
```sql
SELECT * FROM traffic_aggregated_30min 
WHERE window_start > NOW() - INTERVAL '1 hour'
ORDER BY window_start DESC;
```

**Redis:**
```bash
redis-cli GET cityflow:analytics:realtime
```

4. **Verify data consistency**
- Check timestamps match
- Verify aggregation logic (sum of vehicles, average speed)
- Confirm all road segments present

✅ **Phase 8 Complete:** End-to-end flow validated!

---

## 🛑 Stopping Everything

### Graceful Shutdown

```bash
# 1. Stop Spark jobs
cd data-processing/spark-streaming
./submit-jobs.sh --stop

# 2. Stop Spark cluster
docker-compose down

# 3. Stop Schema Registry
cd ../schemas
docker-compose down

# Optional: Remove all data
docker-compose down -v
```

---

## 🐛 Troubleshooting

### Issue: Jobs Not Starting

**Symptom:** Jobs submit but immediately fail

**Solutions:**
```bash
# Check logs
docker logs cityflow-spark-master

# Verify JAR exists
ls -lh target/spark-streaming-jobs-1.0.0.jar

# Check Kafka connectivity
docker exec cityflow-spark-master \
  nc -zv cityflow-kafka-schema 9094
```

### Issue: No Data in Outputs

**Symptom:** Jobs running but no data written

**Solutions:**
```bash
# Check Kafka has data
docker exec cityflow-kafka-schema \
  kafka-console-consumer \
  --bootstrap-server localhost:9094 \
  --topic traffic.reading.events \
  --from-beginning \
  --max-messages 1

# Check Spark UI for errors
open http://localhost:4040

# Check checkpoint location
ls -la /tmp/cityflow/checkpoints/
```

### Issue: High Latency

**Symptom:** Processing time > trigger interval

**Solutions:**
```bash
# Increase executor resources in submit script
# Edit submit-jobs.sh:
EXECUTOR_MEMORY="4g"
EXECUTOR_CORES="4"
NUM_EXECUTORS="4"

# Resubmit jobs
./submit-jobs.sh --stop
./submit-jobs.sh
```

---

## ✅ Success Criteria

Your testing is successful when:

- [x] All Docker containers running (Schema Registry + Spark)
- [x] All 5 schemas registered in Schema Registry
- [x] All 4 Spark jobs running without errors
- [x] Data flowing through Kafka topics
- [x] Delta Lake files being created and updated
- [x] PostgreSQL table receiving aggregated data
- [x] MongoDB collection updated with bus positions
- [x] Redis cache contains analytics data
- [x] Spark UI shows healthy metrics
- [x] No exceptions in Spark logs
- [x] Processing time < trigger interval

---

## 📊 Test Results Template

Document your testing results:

```
=== CityFlow Data Processing Test Results ===

Date: __________
Tester: __________

✅ Infrastructure
- Schema Registry: [ ] Running
- Kafka: [ ] Running
- Spark Cluster: [ ] Running (__ workers)

✅ Schema Registration
- Schemas Registered: [ ] 5/5
- Schema Registry UI: [ ] Accessible

✅ Spark Jobs
- Jobs Running: [ ] 4/4
- Average Processing Time: ________ seconds
- Input Rate: ________ records/sec

✅ Outputs
- Delta Lake: [ ] Data present
- PostgreSQL: [ ] __ rows
- MongoDB: [ ] __ documents
- Redis: [ ] Cache active

✅ Performance
- CPU Usage: ________ %
- Memory Usage: ________ GB
- Latency: ________ seconds

Issues Encountered:
1. __________
2. __________

Resolution:
1. __________
2. __________

Overall Status: [ ] PASS  [ ] FAIL
```

---

**Testing Complete!** 🎉

You now have a fully functional real-time data processing pipeline running!

**Next Steps:**
- Phase 3: Machine Learning Pipeline
- Phase 4: Airflow Orchestration
- Phase 5: Production Deployment

---

**Maintainer:** CityFlow Development Team  
**Last Updated:** January 2026
