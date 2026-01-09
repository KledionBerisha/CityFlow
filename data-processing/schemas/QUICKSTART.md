# 🚀 CityFlow Schema Registry - Quick Start Guide

Get the Schema Registry up and running in **5 minutes**!

## ✅ Step 1: Start Schema Registry Infrastructure

```bash
# Navigate to schemas directory
cd data-processing/schemas

# Start all services (Zookeeper, Kafka, Schema Registry, UIs)
docker-compose up -d

# Verify all services are running
docker-compose ps
```

**Expected output:**
```
NAME                           STATUS
cityflow-kafka-schema          Up (healthy)
cityflow-schema-registry       Up (healthy)
cityflow-schema-registry-ui    Up
cityflow-zookeeper-schema      Up (healthy)
```

**Services available at:**
- 🔧 Schema Registry API: http://localhost:8081
- 🖥️ Schema Registry UI: http://localhost:8082
- 📊 Kafka Topics UI: http://localhost:8083
- 🌐 REST Proxy: http://localhost:8084

## ✅ Step 2: Validate Schemas

```bash
cd registry

# Make script executable (Linux/Mac)
chmod +x validate-schemas.sh

# Validate all Avro schemas
./validate-schemas.sh
```

**Expected output:**
```
[SUCCESS] Schema is valid: traffic-reading-event.avsc
[SUCCESS] Schema is valid: bus-location-event.avsc
[SUCCESS] Schema is valid: incident-event.avsc
[SUCCESS] Schema is valid: sensor-status-event.avsc
[SUCCESS] Schema is valid: bus-status-event.avsc
```

## ✅ Step 3: Register Schemas

### Linux/Mac:
```bash
chmod +x register-schemas.sh
./register-schemas.sh
```

### Windows (PowerShell):
```powershell
.\register-schemas.ps1
```

**Expected output:**
```
╔════════════════════════════════════════════════════════════╗
║        CityFlow Schema Registry - Registration Tool        ║
╚════════════════════════════════════════════════════════════╝

[SUCCESS] Schema registered successfully with ID: 1
[SUCCESS] Schema registered successfully with ID: 2
[SUCCESS] Schema registered successfully with ID: 3
[SUCCESS] Schema registered successfully with ID: 4
[SUCCESS] Schema registered successfully with ID: 5

Registered Schemas:
  - traffic.reading.events-value (version: 1, id: 1)
  - bus.location.events-value (version: 1, id: 2)
  - incident.events-value (version: 1, id: 3)
  - sensor.status.events-value (version: 1, id: 4)
  - bus.status.events-value (version: 1, id: 5)
```

## ✅ Step 4: Verify Registration

### Option 1: Using cURL
```bash
# List all registered schemas
curl -X GET http://localhost:8081/subjects

# Get specific schema
curl -X GET http://localhost:8081/subjects/traffic.reading.events-value/versions/latest | jq
```

### Option 2: Using Schema Registry UI
1. Open http://localhost:8082
2. Click on any subject name
3. View schema details, versions, and compatibility

### Option 3: Using Kafka Topics UI
1. Open http://localhost:8083
2. Browse topics
3. View messages and schemas

## ✅ Step 5: Test with Examples

### Java Example

```bash
cd ../examples/java

# Compile and generate Avro classes
mvn clean compile

# Terminal 1: Start Consumer
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingConsumer"

# Terminal 2: Start Producer
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingProducer"
```

### Python Example

```bash
cd ../examples/python

# Install dependencies
pip install -r requirements.txt

# Terminal 1: Start Consumer
python traffic_reading_consumer.py

# Terminal 2: Start Producer
python traffic_reading_producer.py
```

## 🎉 Success!

You should now see:
- ✅ Producer sending traffic reading events
- ✅ Consumer receiving and processing events
- ✅ Schemas registered and validated
- ✅ Real-time data flowing through Kafka

## 🔧 Common Commands

### List all schemas
```bash
./register-schemas.sh --list
```

### Delete all schemas (dev only)
```bash
./register-schemas.sh --delete
```

### View logs
```bash
docker-compose logs -f schema-registry
```

### Restart services
```bash
docker-compose restart
```

### Stop all services
```bash
docker-compose down
```

### Stop and remove all data
```bash
docker-compose down -v
```

## 🐛 Troubleshooting

### Services not starting?
```bash
# Check logs
docker-compose logs

# Ensure ports are not in use
netstat -an | grep 8081  # Schema Registry
netstat -an | grep 9094  # Kafka
```

### Schema registration fails?
```bash
# Wait for services to be healthy (30-60 seconds)
docker-compose ps

# Check Schema Registry health
curl http://localhost:8081/
```

### Connection refused errors?
```bash
# Verify Kafka is accessible
docker-compose exec kafka-schema kafka-topics --list --bootstrap-server localhost:9094
```

## 📚 What's Next?

1. ✅ **Integrate with Backend Services**
   - Update Spring Boot microservices to use Schema Registry
   - Replace JSON serialization with Avro

2. ✅ **Implement Spark Jobs** (Step 2)
   - Create Spark Structured Streaming jobs
   - Process events in real-time
   - Write to Delta Lake

3. ✅ **Add ML Pipeline** (Step 3)
   - Feature engineering from events
   - Train prediction models
   - Serve predictions via API

4. ✅ **Orchestrate with Airflow** (Step 4)
   - Schedule batch jobs
   - Automate model retraining
   - Data quality monitoring

## 🎓 Learning Resources

- **Avro Tutorial:** https://avro.apache.org/docs/current/
- **Schema Registry Guide:** https://docs.confluent.io/platform/current/schema-registry/
- **Kafka Streams:** https://kafka.apache.org/documentation/streams/

---

**Need Help?** Check the main README.md or examples/README.md for detailed documentation.
