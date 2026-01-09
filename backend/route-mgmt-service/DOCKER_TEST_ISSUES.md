# Docker Test Issues - Troubleshooting Guide

## Problem

Maven tests fail with error: `Could not find a valid Docker environment`

```
ERROR org.testcontainers.dockerclient.DockerClientProviderStrategy -- Could not find a valid Docker environment
NpipeSocketClientProviderStrategy: failed with exception BadRequestException (Status 400: {"ID":"","Containers":0...})
```

## Root Cause

**Docker Desktop 29.x on Windows has a bug** where the named pipe API (`\\.\pipe\docker_cli`) returns malformed responses with empty fields, preventing Testcontainers from connecting properly.

- **Issue**: Named pipe returning Status 400 with empty JSON fields
- **Affected**: Docker Desktop 29.0.0+ on Windows
- **Docker CLI works fine**: The issue is specific to the API endpoint that Testcontainers uses

## Solutions

### Solution 1: Enable TCP Access in Docker Desktop ⭐ (RECOMMENDED)

This is the quickest and most reliable fix:

1. **Open Docker Desktop**
2. Click the **Settings/Gear icon** (⚙️)
3. Go to **"General"** tab
4. **Enable**: ☑️ **"Expose daemon on tcp://localhost:2375 without TLS"**
5. Click **"Apply & Restart"**
6. Wait for Docker to restart (30-60 seconds)
7. Run tests again:
   ```powershell
   cd backend\route-mgmt-service
   mvn clean test
   ```

**Why this works**: Testcontainers will use the TCP connection instead of the buggy named pipe.

**Security Note**: Only enable this on development machines. Do not use in production or on shared networks.

---

### Solution 2: Run Tests from WSL2

Since Docker Desktop uses WSL2, you can run tests directly from WSL where Docker works better:

```powershell
# Open WSL
wsl

# Navigate to project (Windows drives are mounted at /mnt/)
cd /mnt/c/Users/Asus/OneDrive/Desktop/CityFlow/backend/route-mgmt-service

# Run tests
mvn clean test
```

**Why this works**: WSL2 accesses Docker via Unix socket which doesn't have the named pipe bug.

---

### Solution 3: Use the PowerShell Test Runner Script

The project includes a `run-tests.ps1` script that checks Docker status:

```powershell
cd backend\route-mgmt-service
.\run-tests.ps1
```

**Note**: This script may need Solution 1 or 2 to be applied first.

---

### Solution 4: Downgrade Docker Desktop (Not Recommended)

If the above solutions don't work, you can downgrade to Docker Desktop 28.x:

1. Uninstall Docker Desktop 29.x
2. Download Docker Desktop 28.x from [Docker's release archive](https://docs.docker.com/desktop/release-notes/)
3. Install the older version
4. Run tests

**Why not recommended**: You'll miss out on latest features and security updates.

---

## Verification

After applying any solution, verify Docker connectivity:

```powershell
# Check Docker is running
docker ps

# Check Docker info
docker info

# Run tests
mvn clean test
```

You should see:
```
[INFO] BUILD SUCCESS
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

---

## Additional Notes

### Configuration Files

The project includes:
- `src/test/resources/testcontainers.properties` - Testcontainers configuration
- `src/test/resources/application-test.yml` - Test application config

### Testcontainers Version

Current version: **1.19.8**
- Compatible with most Docker versions
- If still having issues, can try version 1.20.x

### Alternative: Skip Tests During Build

If you just want to build without tests:

```powershell
mvn clean package -DskipTests
```

---

## References

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Docker Desktop for Windows](https://docs.docker.com/desktop/windows/)
- [Testcontainers GitHub Issues](https://github.com/testcontainers/testcontainers-java/issues)

---

## Quick Checklist

- [ ] Docker Desktop is running (`docker ps` works)
- [ ] Docker Desktop version checked (`docker version`)
- [ ] TCP exposure enabled in Docker Desktop settings
- [ ] Or running tests from WSL2
- [ ] Maven and Java 17+ installed
- [ ] Tests run successfully

---

Last updated: January 8, 2026
