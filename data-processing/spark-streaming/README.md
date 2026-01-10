# CityFlow Spark Streaming Jobs

## 📋 Overview

This module contains **Apache Spark Structured Streaming** jobs for real-time data processing in the CityFlow system. All jobs consume events from Kafka (using Avro schemas from Schema Registry) and output to various sinks including Delta Lake, PostgreSQL, MongoDB, and Redis.

## 🏗️ Architecture

```
Kafka Topics (Avro)
       ↓
   Spark Streaming Jobs
       ↓
├── Delta Lake (ACID transactions)
├── PostgreSQL (aggregated metrics)
├── MongoDB (current positions)
└── Redis (real-time cache)
```

## 📊 Implemented Jobs

### 1. **Data Lake Ingestion Job** 📥
**Purpose:** Persist all raw events to Delta Lake for analytics

**Features:**
- Multi-topic ingestion (traffic, bus, incident events)
- Data quality validation
- Partitioning by date, hour, and domain-specific fields
- Schema evolution support
- ACID transactions

**Output:**
- `delta-lake/traffic/raw/` - Partitioned by date/hour
- `delta-lake/bus/raw/` - Partitioned by date/hour/routeId
- `delta-lake/incident/raw/` - Partitioned by date/severity

### 2. **Traffic Aggregation Job** 📈
**Purpose:** Calculate rolling traffic metrics over multiple time windows

**Features:**
- 5, 15, and 30-minute rolling windows
- Road segment aggregations
- Speed metrics (avg, min, max, stddev)
- Vehicle count totals
- Congestion distribution analysis
- Incident tracking

**Output:**
- Delta Lake: `traffic/aggregated_{5min|15min|30min}/`
- PostgreSQL: `traffic_aggregated_30min` table

### 3. **Bus ETL Job** 🚌
**Purpose:** Process bus locations with geospatial analysis

**Features:**
- Haversine distance calculations
- Speed calculation from consecutive positions
- Bearing/heading computation
- Delay detection
- Anomaly identification (stopped buses, speed spikes)
- Occupancy percentage

**Output:**
- MongoDB: `bus_positions` collection (current state)
- Real-time geospatial analytics

### 4. **Real-Time Analytics Job** 📊
**Purpose:** Generate live KPIs for dashboards

**Features:**
- City-wide average speed
- Total vehicles on roads
- Congestion distribution
- Active incident count
- Top congested road segments
- Active sensor count

**Output:**
- Redis: `cityflow:analytics:realtime` (60s TTL)

## 🚀 Quick Start

### Prerequisites

- Java 17
- Maven 3.8+
- Docker & Docker Compose
- Running Kafka + Schema Registry (from Step 1)
- Running backend services (data producers)

### 1. Build the Project

```bash
# Navigate to spark-streaming directory
cd data-processing/spark-streaming

# Compile and generate Avro classes
mvn clean compile

# Package JAR with dependencies
mvn package

# Verify JAR creation
ls -lh target/spark-streaming-jobs-1.0.0.jar
```

### 2. Start Spark Cluster

```bash
# Start Spark master and workers
docker-compose up -d

# Verify all services are running
docker-compose ps

# Check Spark Master UI
open http://localhost:8080
```

**Services:**
- Spark Master UI: http://localhost:8080
- Spark Worker 1 UI: http://localhost:8081
- Spark Worker 2 UI: http://localhost:8082
- Spark History Server: http://localhost:18080

### 3. Submit Jobs

**Linux/Mac:**
```bash
chmod +x submit-jobs.sh

# Submit all jobs
./submit-jobs.sh

# Submit specific job
./submit-jobs.sh traffic
./submit-jobs.sh bus
./submit-jobs.sh data-lake
./submit-jobs.sh analytics

# List running jobs
./submit-jobs.sh --list

# Stop all jobs
./submit-jobs.sh --stop
```

**Windows:**
```powershell
# Submit all jobs
.\submit-jobs.ps1

# Submit specific job
.\submit-jobs.ps1 traffic

# List running jobs
.\submit-jobs.ps1 -List

# Stop all jobs
.\submit-jobs.ps1 -Stop
```

## 📁 Project Structure

```
spark-streaming/
├── src/main/java/com/cityflow/spark/
│   ├── config/
│   │   └── ConfigLoader.java           # Configuration management
│   ├── jobs/
│   │   ├── DataLakeIngestionJob.java   # Raw event ingestion
│   │   ├── TrafficAggregationJob.java  # Traffic metrics
│   │   ├── BusETLJob.java              # Bus geospatial processing
│   │   └── RealTimeAnalyticsJob.java   # Live KPIs
│   └── utils/
│       ├── AvroDeserializer.java       # Schema Registry integration
│       ├── DeltaLakeWriter.java        # Delta Lake operations
│       ├── DeadLetterQueueHandler.java # Error handling
│       └── GeospatialUtils.java        # Distance/bearing calculations
├── src/main/resources/
│   ├── application.conf                # Configuration
│   └── log4j2.properties              # Logging
├── target/
│   └── spark-streaming-jobs-1.0.0.jar # Fat JAR
├── docker-compose.yml                  # Spark cluster
├── submit-jobs.sh                     # Job submission (Linux/Mac)
├── submit-jobs.ps1                    # Job submission (Windows)
├── pom.xml                            # Maven configuration
└── README.md                          # This file
```

## ⚙️ Configuration

All configuration is in `src/main/resources/application.conf`:

### Kafka Settings
```hocon
cityflow.kafka {
  bootstrap-servers = "localhost:9093"
  schema-registry-url = "http://localhost:8081"
  topics {
    traffic-reading = "traffic.reading.events"
    bus-location = "bus.location.events"
    incident = "incident.events"
  }
}
```

### Delta Lake Settings
```hocon
cityflow.delta-lake {
  base-path = "/tmp/cityflow/delta-lake"
  checkpoint-location = "/tmp/cityflow/checkpoints"
}
```

### Streaming Settings
```hocon
cityflow.streaming {
  trigger {
    traffic-aggregation = "30 seconds"
    bus-etl = "15 seconds"
    data-lake = "1 minute"
    analytics = "30 seconds"
  }
  watermark {
    traffic = "5 minutes"
    bus = "2 minutes"
    incident = "1 minute"
  }
  windows {
    short = "5 minutes"
    medium = "15 minutes"
    long = "30 minutes"
  }
}
```

## 🧪 Testing

### Manual Testing

1. **Start all infrastructure:**
```bash
# Step 1: Schema Registry
cd ../schemas
docker-compose up -d

# Step 2: Spark Cluster
cd ../spark-streaming
docker-compose up -d

# Verify services
docker ps
```

2. **Build and submit jobs:**
```bash
mvn clean package
./submit-jobs.sh
```

3. **Generate test data:**
```bash
# Use backend services or schema examples
cd ../../schemas/examples/python
python traffic_reading_producer.py
```

4. **Monitor execution:**
- Spark UI: http://localhost:8080
- Check job progress
- View streaming queries
- Monitor resource usage

5. **Verify outputs:**

**Delta Lake:**
```bash
# List files in Delta Lake
ls -R /tmp/cityflow/delta-lake/

# Check Spark for data
spark-shell
scala> spark.read.format("delta").load("/tmp/cityflow/delta-lake/traffic/raw").show()
```

**PostgreSQL:**
```bash
psql -h localhost -U postgres -d cityflow
SELECT * FROM traffic_aggregated_30min LIMIT 10;
```

**MongoDB:**
```bash
mongosh mongodb://localhost:27017/cityflow
db.bus_positions.find().limit(10)
```

**Redis:**
```bash
redis-cli
GET cityflow:analytics:realtime
```

### Integration Testing

Run the full pipeline end-to-end:

1. Backend produces events → Kafka
2. Spark consumes from Kafka
3. Data written to all sinks
4. Verify data consistency

## 📈 Monitoring

### Spark UI
- **Master:** http://localhost:8080
  - Worker status
  - Running applications
  - Resource allocation

- **Application UI:** http://localhost:4040 (when job is running)
  - SQL queries
  - Streaming statistics
  - Stage progress

- **History Server:** http://localhost:18080
  - Completed applications
  - Historical metrics

### Metrics to Monitor

1. **Processing Rate**
   - Records/second per job
   - Batch processing time
   - End-to-end latency

2. **Resource Usage**
   - Executor memory
   - CPU utilization
   - Disk I/O

3. **Data Quality**
   - Null value percentage
   - Schema conformance
   - Late data count

4. **Checkpoints**
   - Checkpoint size
   - Checkpoint duration
   - Recovery time

## 🐛 Troubleshooting

### Job Fails to Start

**Issue:** `ClassNotFoundException`
```
Solution: Ensure all dependencies are packaged in JAR
mvn clean package
```

**Issue:** `Connection refused to Kafka`
```
Solution: Check Kafka is running and accessible
docker ps | grep kafka
Update bootstrap-servers in application.conf
```

### Performance Issues

**Issue:** High latency
```
Solution: 
- Increase number of executors
- Tune batch interval
- Add more Kafka partitions
- Optimize shuffle operations
```

**Issue:** Out of memory
```
Solution:
- Increase executor memory
- Reduce maxOffsetsPerTrigger
- Enable checkpointing compression
```

### Data Quality Issues

**Issue:** Duplicate records
```
Solution:
- Check exactly-once semantics
- Verify idempotent writes
- Add deduplication logic
```

**Issue:** Late data
```
Solution:
- Increase watermark duration
- Check producer timestamp accuracy
- Monitor clock skew
```

## 🔧 Advanced Features

### Dead Letter Queue

Failed messages are sent to DLQ topic:
```java
DeadLetterQueueHandler dlqHandler = new DeadLetterQueueHandler(
    bootstrapServers,
    "dlq.events"
);
dlqHandler.sendToDLQ(failedMessages, topic, reason, jobName);
```

### Delta Lake Operations

**Optimize:**
```bash
spark-shell
scala> import io.delta.tables._
scala> val table = DeltaTable.forPath("/tmp/cityflow/delta-lake/traffic/raw")
scala> table.optimize().executeCompaction()
```

**Vacuum:**
```bash
scala> table.vacuum(168) // Remove files older than 7 days
```

**Time Travel:**
```bash
scala> spark.read.format("delta")
      .option("versionAsOf", 5)
      .load("/tmp/cityflow/delta-lake/traffic/raw")
      .show()
```

### Geospatial Analysis

Custom UDFs for distance and bearing:
```java
spark.udf().register("calculate_distance",
    (lat1, lon1, lat2, lon2) -> 
        GeospatialUtils.calculateDistance(lat1, lon1, lat2, lon2)
);
```

## 📚 Dependencies

Key libraries used:

- **Apache Spark 3.5.0** - Streaming engine
- **Delta Lake 3.0.0** - ACID transactions
- **Kafka 3.6.0** - Event streaming
- **Avro 1.11.3** - Serialization
- **Confluent 7.5.0** - Schema Registry
- **PostgreSQL 42.7.1** - JDBC driver
- **MongoDB Spark Connector 4.11.1** - MongoDB integration
- **Jedis 5.1.0** - Redis client

## 🎓 Academic Considerations

This implementation demonstrates:

1. **Real-time Stream Processing**
   - Windowing and aggregations
   - Stateful operations
   - Late data handling

2. **Data Engineering Best Practices**
   - Schema management
   - Data quality validation
   - Partitioning strategies
   - ACID guarantees

3. **Geospatial Computing**
   - Haversine formula
   - Bearing calculations
   - Distance-based analytics

4. **Production Readiness**
   - Error handling (DLQ)
   - Monitoring and observability
   - Resource management
   - Fault tolerance

---

**Maintainer:** CityFlow Development Team  
**Last Updated:** January 2026  
**Version:** 1.0.0
