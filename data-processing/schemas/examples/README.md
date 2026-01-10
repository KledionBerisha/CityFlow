# CityFlow Schema Integration Examples

This directory contains **working examples** of how to integrate CityFlow Avro schemas with Kafka producers and consumers in **Java** and **Python**.

## 📁 Structure

```
examples/
├── java/
│   ├── pom.xml                          # Maven configuration
│   ├── TrafficReadingProducer.java      # Producer example
│   └── TrafficReadingConsumer.java      # Consumer example
├── python/
│   ├── requirements.txt                 # Python dependencies
│   ├── traffic_reading_producer.py      # Producer example
│   └── traffic_reading_consumer.py      # Consumer example
└── README.md                            # This file
```

## ☕ Java Examples

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- Running Kafka and Schema Registry (see parent README)

### Setup

1. **Navigate to Java examples directory:**
```bash
cd java
```

2. **Generate Avro classes from schemas:**
```bash
mvn clean compile
```

This will:
- Read Avro schemas from `../../avro/`
- Generate Java classes in `target/generated-sources/avro/`
- Compile all code

3. **Build executable JAR:**
```bash
mvn package
```

### Running the Producer

**Using Maven:**
```bash
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingProducer"
```

**Using compiled JAR:**
```bash
java -cp target/schema-examples-java-1.0.0.jar com.cityflow.examples.TrafficReadingProducer
```

**Expected Output:**
```
[INFO] Starting Traffic Reading Producer...
[INFO] Sent event to partition 2 with offset 0
[INFO] Sent event to partition 1 with offset 0
...
[INFO] All messages sent successfully!
```

### Running the Consumer

**In a separate terminal:**
```bash
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingConsumer"
```

**Expected Output:**
```
[INFO] Started Traffic Reading Consumer. Waiting for messages...
========================================
[INFO] Received Traffic Reading Event:
[INFO]   Partition: 2, Offset: 0
[INFO]   Sensor: SENSOR-001 (sensor-id-1)
[INFO]   Average Speed: 67.34 km/h
[INFO]   Congestion Level: LIGHT
...
```

### Key Features Demonstrated

- ✅ Avro serialization/deserialization
- ✅ Schema Registry integration
- ✅ Automatic schema generation from .avsc files
- ✅ Type-safe event handling
- ✅ Error handling and logging
- ✅ Delivery callbacks
- ✅ Business logic examples (congestion detection, incident alerts)

## 🐍 Python Examples

### Prerequisites

- Python 3.8 or higher
- pip
- Running Kafka and Schema Registry

### Setup

1. **Navigate to Python examples directory:**
```bash
cd python
```

2. **Create virtual environment (recommended):**
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
.\venv\Scripts\activate   # Windows
```

3. **Install dependencies:**
```bash
pip install -r requirements.txt
```

### Running the Producer

```bash
python traffic_reading_producer.py
```

**Expected Output:**
```
2026-01-09 14:30:00 - __main__ - INFO - Starting Traffic Reading Producer (Python)...
2026-01-09 14:30:00 - __main__ - INFO - Avro schema loaded successfully
============================================================
2026-01-09 14:30:01 - __main__ - INFO - Producing event 1/10:
2026-01-09 14:30:01 - __main__ - INFO -   Sensor: SENSOR-001
2026-01-09 14:30:01 - __main__ - INFO -   Speed: 67.34 km/h
...
```

### Running the Consumer

**In a separate terminal:**
```bash
python traffic_reading_consumer.py
```

**Expected Output:**
```
2026-01-09 14:30:10 - __main__ - INFO - Starting Traffic Reading Consumer (Python)...
2026-01-09 14:30:10 - __main__ - INFO - Subscribed to topic: traffic.reading.events
============================================================
2026-01-09 14:30:11 - __main__ - INFO - Received Traffic Reading Event:
2026-01-09 14:30:11 - __main__ - INFO -   Sensor: SENSOR-001 (sensor-id-1)
2026-01-09 14:30:11 - __main__ - INFO -   Average Speed: 67.34 km/h
...
```

### Key Features Demonstrated

- ✅ Avro schema loading from .avsc files
- ✅ Schema Registry integration
- ✅ Event serialization/deserialization
- ✅ Delivery callbacks
- ✅ Error handling
- ✅ Business logic examples
- ✅ Logging and monitoring

## 🎯 Key Concepts Explained

### 1. Schema Registry Integration

**Java:**
```java
props.put("schema.registry.url", "http://localhost:8081");
props.put("auto.register.schemas", "true");
```

**Python:**
```python
config = {
    'schema.registry.url': 'http://localhost:8081',
}
```

### 2. Avro Serialization

**Java** uses generated classes:
```java
TrafficReadingEvent event = TrafficReadingEvent.newBuilder()
    .setEventId(UUID.randomUUID().toString())
    .setSensorCode("SENSOR-001")
    .setAverageSpeed(67.5)
    .build();
```

**Python** uses dictionaries:
```python
event = {
    "eventId": str(uuid.uuid4()),
    "sensorCode": "SENSOR-001",
    "averageSpeed": 67.5,
}
```

### 3. Message Keys

Both examples use `sensorCode` as the message key to ensure:
- Events from the same sensor go to the same partition
- Ordering is maintained per sensor
- Load is distributed across partitions

### 4. Error Handling

**Java:**
```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        logger.error("Error sending message", exception);
    }
});
```

**Python:**
```python
def delivery_callback(err, msg):
    if err:
        logger.error(f'Message delivery failed: {err}')
```

## 🧪 Testing

### Test Complete Workflow

1. **Start infrastructure:**
```bash
cd ../../
docker-compose up -d
```

2. **Register schemas:**
```bash
cd registry
./register-schemas.sh
```

3. **Start consumer (Terminal 1):**
```bash
# Java
cd examples/java
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingConsumer"

# Python
cd examples/python
python traffic_reading_consumer.py
```

4. **Start producer (Terminal 2):**
```bash
# Java
cd examples/java
mvn exec:java -Dexec.mainClass="com.cityflow.examples.TrafficReadingProducer"

# Python
cd examples/python
python traffic_reading_producer.py
```

### Verify in Schema Registry UI

Open http://localhost:8082 and verify:
- Schema is registered
- Version is correct
- Compatibility level is set

### Verify in Kafka Topics UI

Open http://localhost:8083 and verify:
- Messages are being produced
- Partitions are balanced
- Offsets are advancing

## 🔧 Customization

### Modify Event Structure

1. Update the Avro schema in `../../avro/traffic-reading-event.avsc`
2. Regenerate Java classes: `mvn clean compile`
3. Python automatically uses updated schema
4. Re-register schema: `./registry/register-schemas.sh`

### Add New Event Types

1. Create new .avsc file in `../../avro/`
2. Add to registration script
3. Create producer/consumer classes
4. Follow same patterns as TrafficReadingEvent

### Configure Different Kafka Cluster

Update constants in the code:
```java
private static final String BOOTSTRAP_SERVERS = "your-kafka:9092";
private static final String SCHEMA_REGISTRY_URL = "http://your-registry:8081";
```

## 📊 Performance Tuning

### Producer Optimization

**Java:**
```java
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
```

**Python:**
```python
config = {
    'compression.type': 'snappy',
    'batch.size': 16384,
    'linger.ms': 10,
}
```

### Consumer Optimization

**Java:**
```java
props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
```

**Python:**
```python
config = {
    'max.poll.records': 500,
    'fetch.min.bytes': 1024,
}
```

## 🐛 Troubleshooting

### Schema Not Found Error
```
Error: Schema not found in registry
```
**Solution:** Run `./registry/register-schemas.sh`

### Connection Refused
```
Error: Connection refused to localhost:9094
```
**Solution:** Ensure Kafka is running: `docker-compose ps`

### Avro Generation Failed (Java)
```
Error: Could not generate sources
```
**Solution:** Check schema syntax: `./registry/validate-schemas.sh`

### Import Error (Python)
```
ImportError: No module named 'confluent_kafka'
```
**Solution:** Install dependencies: `pip install -r requirements.txt`

## 📚 Additional Resources

- [Confluent Kafka Python](https://docs.confluent.io/kafka-clients/python/current/overview.html)
- [Kafka Avro Serializer](https://docs.confluent.io/platform/current/schema-registry/serdes-develop/index.html)
- [Avro Java Documentation](https://avro.apache.org/docs/current/gettingstartedjava.html)
- [Maven Avro Plugin](https://avro.apache.org/docs/current/gettingstartedjava.html#Serializing+and+deserializing+with+code+generation)

---

**Next Steps:**
- Implement other event types (BusLocationEvent, IncidentEvent, etc.)
- Add batch processing examples
- Implement error handling strategies (DLQ, retry logic)
- Add monitoring and metrics collection
