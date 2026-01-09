# CityFlow Schema Registry

## 📋 Overview

This directory contains **Avro schema definitions** and **Schema Registry infrastructure** for the CityFlow real-time transport monitoring system. All event schemas are centrally managed, versioned, and validated using Confluent Schema Registry.

## 🏗️ Architecture

```
schemas/
├── avro/                          # Avro schema definitions (.avsc)
│   ├── traffic-reading-event.avsc
│   ├── bus-location-event.avsc
│   ├── incident-event.avsc
│   ├── sensor-status-event.avsc
│   └── bus-status-event.avsc
├── registry/                      # Schema Registry configuration
│   ├── config.properties
│   ├── register-schemas.sh       # Bash registration script
│   ├── register-schemas.ps1      # PowerShell registration script
│   └── validate-schemas.sh       # Schema validation tool
├── examples/                      # Integration examples
│   ├── java/                     # Java producer/consumer examples
│   └── python/                   # Python producer/consumer examples
├── docker-compose.yml            # Schema Registry + Kafka setup
└── README.md                     # This file
```

## 📊 Schema Definitions

### 1. Traffic Reading Event
**Topic:** `traffic.reading.events`  
**Schema:** `traffic-reading-event.avsc`  
**Description:** Real-time traffic sensor readings with congestion metrics

**Key Fields:**
- `sensorId`, `sensorCode` - Sensor identification
- `averageSpeed`, `vehicleCount` - Traffic metrics
- `congestionLevel` - Enum: FREE_FLOW, LIGHT, MODERATE, HEAVY, SEVERE
- `location` - Geographic coordinates (lat/lon)
- `incidentDetected` - Boolean flag

### 2. Bus Location Event
**Topic:** `bus.location.events`  
**Schema:** `bus-location-event.avsc`  
**Description:** Real-time GPS location and operational status of buses

**Key Fields:**
- `busId`, `vehicleId`, `busCode` - Bus identification
- `location` - GPS coordinates with accuracy
- `speedKmh`, `heading` - Movement metrics
- `occupancy`, `capacity` - Passenger information
- `operationalStatus` - Enum: IN_SERVICE, DELAYED, STOPPED, etc.
- `delayMinutes` - Schedule adherence

### 3. Incident Event
**Topic:** `incident.events`  
**Schema:** `incident-event.avsc`  
**Description:** Traffic incident detection and status updates

**Key Fields:**
- `incidentId`, `incidentCode` - Incident identification
- `type` - Enum: ACCIDENT, CONGESTION, BREAKDOWN, etc.
- `severity` - Enum: LOW, MEDIUM, HIGH, CRITICAL
- `status` - Enum: DETECTED, CONFIRMED, IN_PROGRESS, RESOLVED
- `location` - Geographic location with address
- `confidence` - Detection confidence (0.0 to 1.0)
- `impact` - Affected vehicles, buses, routes, delays

### 4. Sensor Status Event
**Topic:** `sensor.status.events`  
**Schema:** `sensor-status-event.avsc`  
**Description:** Traffic sensor health and status changes

**Key Fields:**
- `sensorId`, `sensorCode` - Sensor identification
- `sensorType` - Enum: SPEED, COUNT, CAMERA, RADAR, etc.
- `oldStatus`, `newStatus` - Status transition
- `healthMetrics` - Battery, signal strength, temperature
- `alertLevel` - Enum: INFO, WARNING, ERROR, CRITICAL

### 5. Bus Status Event
**Topic:** `bus.status.events`  
**Schema:** `bus-status-event.avsc`  
**Description:** Bus operational status changes and fleet management

**Key Fields:**
- `busId`, `vehicleId` - Bus identification
- `oldStatus`, `newStatus` - Status transition
- `driverInfo` - Driver assignment details
- `vehicleMetrics` - Fuel, engine temperature, diagnostics
- `requiresAttention` - Boolean flag for alerts

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- `jq` (for schema validation)
- `curl` (for API calls)

### 1. Start Schema Registry

```bash
# Navigate to schemas directory
cd data-processing/schemas

# Start all services
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f schema-registry
```

**Services will be available at:**
- Schema Registry API: http://localhost:8081
- Schema Registry UI: http://localhost:8082
- Kafka Topics UI: http://localhost:8083
- REST Proxy: http://localhost:8084

### 2. Validate Schemas

```bash
# Validate all schemas
cd registry
chmod +x validate-schemas.sh
./validate-schemas.sh
```

### 3. Register Schemas

**Linux/Mac:**
```bash
cd registry
chmod +x register-schemas.sh
./register-schemas.sh
```

**Windows (PowerShell):**
```powershell
cd registry
.\register-schemas.ps1
```

**Additional Commands:**
```bash
# List all registered schemas
./register-schemas.sh --list

# Delete all schemas (development only)
./register-schemas.sh --delete
```

### 4. Verify Registration

```bash
# List all subjects
curl -X GET http://localhost:8081/subjects

# Get specific schema
curl -X GET http://localhost:8081/subjects/traffic.reading.events-value/versions/latest

# Check compatibility level
curl -X GET http://localhost:8081/config/traffic.reading.events-value
```

## 🔄 Schema Versioning Strategy

### Compatibility Levels

CityFlow uses **BACKWARD** compatibility by default:
- ✅ New schema can read data written by old schema
- ✅ Safe to update consumers before producers
- ✅ Fields can be added with defaults
- ❌ Fields cannot be removed

**Compatibility Matrix:**

| Change Type | Backward | Forward | Full |
|-------------|----------|---------|------|
| Add field with default | ✅ | ❌ | ❌ |
| Remove field | ❌ | ✅ | ❌ |
| Add field without default | ❌ | ❌ | ❌ |
| Rename field | ❌ | ❌ | ❌ |
| Change field type | ❌ | ❌ | ❌ |

### Version Evolution Rules

1. **Adding Fields**
   ```json
   {
     "name": "newField",
     "type": "string",
     "default": "default-value"  // Default is REQUIRED
   }
   ```

2. **Making Fields Optional**
   ```json
   {
     "name": "optionalField",
     "type": ["null", "string"],  // Union type with null
     "default": null
   }
   ```

3. **Adding Enum Values**
   ```json
   {
     "type": "enum",
     "name": "Status",
     "symbols": ["OLD_VALUE", "NEW_VALUE"]  // Append only
   }
   ```

4. **Deprecated Fields**
   ```json
   {
     "name": "deprecatedField",
     "type": "string",
     "doc": "DEPRECATED: Use newField instead. Will be removed in v3.0"
   }
   ```

### Breaking Changes

Breaking changes require:
1. **New major version** of the schema
2. **New topic** with version suffix (e.g., `traffic.reading.events.v2`)
3. **Dual publishing** during migration period
4. **Consumer migration plan**

## 📐 Schema Design Best Practices

### 1. Documentation
- Every schema, field, and enum must have a `doc` attribute
- Document default values and their meaning
- Mark deprecated fields clearly

### 2. Naming Conventions
- **Schemas:** PascalCase (e.g., `TrafficReadingEvent`)
- **Fields:** camelCase (e.g., `vehicleCount`)
- **Enums:** UPPER_SNAKE_CASE (e.g., `FREE_FLOW`)
- **Namespaces:** Reverse domain (e.g., `com.cityflow.events.traffic`)

### 3. Type Selection
- Use `long` with `logicalType: timestamp-millis` for timestamps
- Use `double` for floating-point numbers (speed, coordinates)
- Use enums for fixed sets of values
- Use unions `["null", "type"]` for optional fields

### 4. Default Values
- Always provide defaults for optional fields
- Use meaningful defaults (not just null)
- Document the semantics of default values

### 5. Geospatial Data
```json
{
  "type": "record",
  "name": "GeoLocation",
  "fields": [
    {"name": "latitude", "type": "double"},
    {"name": "longitude", "type": "double"},
    {"name": "altitude", "type": ["null", "double"], "default": null}
  ]
}
```

## 🔧 Integration Examples

### Java Producer (with Avro)

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9094");
props.put("key.serializer", StringSerializer.class);
props.put("value.serializer", KafkaAvroSerializer.class);
props.put("schema.registry.url", "http://localhost:8081");

KafkaProducer<String, TrafficReadingEvent> producer = new KafkaProducer<>(props);

TrafficReadingEvent event = TrafficReadingEvent.newBuilder()
    .setEventId(UUID.randomUUID().toString())
    .setSensorId("SENSOR-001")
    .setAverageSpeed(45.5)
    .setCongestionLevel(CongestionLevel.MODERATE)
    .build();

producer.send(new ProducerRecord<>("traffic.reading.events", event));
```

### Python Consumer (with Avro)

```python
from confluent_kafka import Consumer
from confluent_kafka.avro import AvroConsumer

consumer = AvroConsumer({
    'bootstrap.servers': 'localhost:9094',
    'group.id': 'data-processing-group',
    'schema.registry.url': 'http://localhost:8081'
})

consumer.subscribe(['traffic.reading.events'])

for message in consumer:
    event = message.value()
    print(f"Sensor: {event['sensorCode']}, Speed: {event['averageSpeed']}")
```

## 🧪 Testing Schemas

### Validate Schema Syntax
```bash
cd registry
./validate-schemas.sh
```

### Test Schema Compatibility
```bash
# Check if new schema is compatible with existing version
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data @new-schema.json \
  http://localhost:8081/compatibility/subjects/traffic.reading.events-value/versions/latest
```

### Generate Test Data
```bash
# Use avro-tools to generate sample data
java -jar avro-tools.jar random \
  --schema traffic-reading-event.avsc \
  --count 10 \
  test-data.avro
```

## 🛠️ Maintenance

### Backup Schemas
```bash
# Export all schemas
curl -X GET http://localhost:8081/subjects | jq -r '.[]' | while read subject; do
  curl -X GET "http://localhost:8081/subjects/$subject/versions/latest" > "${subject}.json"
done
```

### Update Schema Compatibility
```bash
# Change compatibility level globally
curl -X PUT \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"compatibility": "FULL"}' \
  http://localhost:8081/config
```

### Monitor Schema Usage
```bash
# Get schema versions
curl -X GET http://localhost:8081/subjects/traffic.reading.events-value/versions

# Get schema by ID
curl -X GET http://localhost:8081/schemas/ids/1
```

## 🐛 Troubleshooting

### Schema Registration Fails
```bash
# Check Schema Registry logs
docker-compose logs schema-registry

# Verify Kafka connectivity
docker-compose exec kafka-schema kafka-topics --list --bootstrap-server localhost:9094

# Test Schema Registry health
curl http://localhost:8081/
```

### Compatibility Errors
```bash
# Check current compatibility level
curl http://localhost:8081/config

# View incompatible fields
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data @new-schema.json \
  http://localhost:8081/compatibility/subjects/traffic.reading.events-value/versions/latest
```

### Reset Development Environment
```bash
# Stop all services
docker-compose down -v

# Remove all data
rm -rf volumes/

# Restart fresh
docker-compose up -d
./registry/register-schemas.sh
```

## 📚 Additional Resources

- [Confluent Schema Registry Documentation](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Avro Specification](https://avro.apache.org/docs/current/spec.html)
- [Schema Evolution Best Practices](https://docs.confluent.io/platform/current/schema-registry/avro.html)
- [Kafka Avro Serialization](https://docs.confluent.io/platform/current/schema-registry/serdes-develop/index.html)

## 🔐 Security Considerations

### Production Deployment
- Enable authentication on Schema Registry
- Use TLS/SSL for all communications
- Implement role-based access control (RBAC)
- Restrict schema modification permissions
- Enable audit logging

### Schema Validation
- Enforce schema registration in CI/CD
- Validate schemas before deployment
- Run compatibility checks automatically
- Monitor schema usage and violations

---

**Maintainer:** CityFlow Development Team  
**Last Updated:** January 2026  
**Version:** 1.0.0
