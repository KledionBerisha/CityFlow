# Testing Guide

## Running Tests

The integration tests use Testcontainers which requires Docker to be running.

### Prerequisites

1. **Docker Desktop must be running**
2. Docker daemon must be accessible
3. Sufficient disk space for pulling images

### Option 1: Fix Docker Connection (Recommended)

#### For Windows:

1. **Start Docker Desktop**
   ```powershell
   # Open Docker Desktop from Start Menu or run:
   start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
   ```

2. **Wait for Docker to start completely** (look for "Docker Desktop is running" in system tray)

3. **Verify Docker is working**
   ```powershell
   docker ps
   docker info
   ```

4. **Run tests**
   ```powershell
   cd backend\route-mgmt-service
   mvn clean test
   ```

#### Common Docker Issues on Windows:

**Issue: "Could not find a valid Docker environment"**

**Solution 1: Restart Docker Desktop**
```powershell
# Stop Docker Desktop
taskkill /IM "Docker Desktop.exe" /F

# Wait 10 seconds
Start-Sleep -Seconds 10

# Start Docker Desktop
start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Wait for Docker to be ready (30-60 seconds)
Start-Sleep -Seconds 30

# Verify
docker ps
```

**Solution 2: Check WSL 2 (Windows Subsystem for Linux)**
```powershell
# Enable WSL 2
wsl --set-default-version 2

# Update WSL
wsl --update

# Restart Docker Desktop
```

**Solution 3: Check Docker Settings**
1. Open Docker Desktop
2. Go to Settings → General
3. Ensure "Use the WSL 2 based engine" is checked (on Windows 10/11 Home)
4. OR "Use Hyper-V backend" is available (on Windows Pro/Enterprise)
5. Click "Apply & Restart"

**Solution 4: Check npipe connection**
```powershell
# Test if Docker daemon is accessible
docker -H npipe:////./pipe/docker_engine version
```

### Option 2: Run Specific Tests Without Docker

If Docker is not available, you can run tests against a local PostgreSQL:

```powershell
# Start PostgreSQL from docker-compose (just the database)
cd C:\Users\Asus\OneDrive\Desktop\CityFlow
docker-compose up -d postgres

# Wait for PostgreSQL to start
Start-Sleep -Seconds 5

# Run tests with local database
cd backend\route-mgmt-service
mvn test -Dspring.profiles.active=local-db
```

### Option 3: Skip Integration Tests

```powershell
# Build without tests
mvn clean package -DskipTests

# Or run only unit tests (no Testcontainers)
mvn test -Dtest=!*IntegrationTest
```

## Troubleshooting

### Check Docker Status
```powershell
docker version
docker info
docker ps -a
```

### View Docker Logs
```
C:\Users\Asus\AppData\Local\Docker\log
```

### Testcontainers Logs
Tests will show detailed logs about why Docker connection failed.

### Still Having Issues?

1. **Reboot your machine** - Sometimes Docker needs a full system restart
2. **Check disk space** - Docker needs space for images
3. **Check antivirus** - Some antivirus software blocks Docker
4. **Reinstall Docker Desktop** - Last resort

## Running Tests Successfully

Once Docker is working:

```powershell
# Full test suite
mvn clean test

# Single test class
mvn test -Dtest=RouteControllerIntegrationTest

# With debug output
mvn test -X

# Skip Testcontainers startup logs
mvn test -Dorg.testcontainers.utility.LogUtils.LOG_LEVEL=OFF
```

## CI/CD Considerations

In CI/CD pipelines (GitHub Actions, GitLab CI), Docker is usually available by default.
Testcontainers will automatically detect and use it.

Example GitHub Actions:
```yaml
- name: Run tests
  run: mvn test
  # Docker is available by default in GitHub Actions runners
```
