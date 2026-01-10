# Route Management Service - Startup Troubleshooting

## Common Startup Failures

### 1. Keycloak Not Running
**Error**: `Failed to initialize Keycloak admin client`

**Solution**:
- Start Keycloak on port 8080
- Or disable Keycloak temporarily: Set `keycloak.enabled=false` in `application.yml`
- Or set environment variable: `KEYCLOAK_ENABLED=false`

### 2. PostgreSQL Database Not Running
**Error**: `Connection refused` or `FATAL: database "cityflow" does not exist`

**Solution**:
- Ensure PostgreSQL is running on port 5433
- Create database: `CREATE DATABASE cityflow;`
- Update credentials in `application.yml` or set environment variables:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/cityflow`
  - `SPRING_DATASOURCE_USERNAME=your_username`
  - `SPRING_DATASOURCE_PASSWORD=your_password`

### 3. Kafka Not Running
**Error**: `Connection to node could not be established`

**Solution**:
- Start Kafka on port 9093
- Or disable Kafka: Set `app.kafka.enabled=false` in `application.yml`

### 4. Port Already in Use
**Error**: `Port 8081 is already in use`

**Solution**:
- Stop the process using port 8081
- Or change port: Set `server.port=8082` in `application.yml`

## Quick Start (Development Mode)

To start without external dependencies:

1. **Disable Keycloak** (for testing):
   ```yaml
   keycloak:
     enabled: false
   ```

2. **Disable Security**:
   ```yaml
   app:
     security:
       enabled: false
   ```

3. **Use H2 Database** (in-memory):
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:mem:cityflow
       driver-class-name: org.h2.Driver
     jpa:
       database-platform: org.hibernate.dialect.H2Dialect
   ```

4. **Disable Kafka**:
   ```yaml
   app:
     kafka:
       enabled: false
   ```

## Check Service Status

Before starting, verify:
- ✅ PostgreSQL is running: `psql -U postgres -c "SELECT 1"`
- ✅ Keycloak is running: `curl http://localhost:8080/realms/cityflow`
- ✅ Kafka is running: `kafka-topics --list --bootstrap-server localhost:9093`
- ✅ Port 8081 is free: `netstat -an | grep 8081`

## Getting Detailed Error Messages

Run with debug logging:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.root=DEBUG"
```

Or check the full stack trace:
```bash
mvn spring-boot:run -e -X
```
