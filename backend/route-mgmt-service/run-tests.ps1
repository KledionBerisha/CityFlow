# PowerShell script to fix Docker and run tests
# Usage: .\run-tests.ps1

Write-Host "CityFlow - Route Management Service Test Runner" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if Docker is running
function Test-Docker {
    try {
        $null = docker ps 2>&1
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

# Function to start Docker Desktop
function Start-DockerDesktop {
    Write-Host "Starting Docker Desktop..." -ForegroundColor Yellow
    
    $dockerPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    
    if (Test-Path $dockerPath) {
        Start-Process $dockerPath
        Write-Host "Waiting for Docker to start (this may take 30-60 seconds)..." -ForegroundColor Yellow
        
        # Wait up to 2 minutes for Docker to start
        $timeout = 120
        $elapsed = 0
        while (-not (Test-Docker) -and $elapsed -lt $timeout) {
            Start-Sleep -Seconds 5
            $elapsed += 5
            Write-Host "." -NoNewline
        }
        Write-Host ""
        
        if (Test-Docker) {
            Write-Host "✓ Docker is now running!" -ForegroundColor Green
            return $true
        }
        else {
            Write-Host "✗ Docker failed to start within timeout" -ForegroundColor Red
            return $false
        }
    }
    else {
        Write-Host "✗ Docker Desktop not found at: $dockerPath" -ForegroundColor Red
        Write-Host "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
        return $false
    }
}

# Check Docker status
Write-Host "Step 1: Checking Docker status..." -ForegroundColor Cyan
if (Test-Docker) {
    Write-Host "✓ Docker is running!" -ForegroundColor Green
}
else {
    Write-Host "✗ Docker is not running" -ForegroundColor Red
    
    $response = Read-Host "Would you like to start Docker Desktop? (Y/n)"
    if ($response -eq "" -or $response -eq "Y" -or $response -eq "y") {
        if (-not (Start-DockerDesktop)) {
            Write-Host ""
            Write-Host "Please start Docker Desktop manually and run this script again." -ForegroundColor Yellow
            exit 1
        }
    }
    else {
        Write-Host ""
        Write-Host "Tests require Docker to be running." -ForegroundColor Yellow
        Write-Host "You can:" -ForegroundColor Yellow
        Write-Host "  1. Start Docker Desktop manually" -ForegroundColor Yellow
        Write-Host "  2. Run: mvn package -DskipTests (to build without tests)" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "Step 2: Testing Docker connectivity..." -ForegroundColor Cyan
try {
    docker version | Out-Null
    Write-Host "✓ Docker connectivity verified!" -ForegroundColor Green
}
catch {
    Write-Host "✗ Docker connectivity issue" -ForegroundColor Red
    Write-Host "Try running Docker Desktop as Administrator" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Step 3: Running Maven tests..." -ForegroundColor Cyan
Write-Host "This will download PostgreSQL test image (first time only)" -ForegroundColor Yellow
Write-Host ""

# Clear any stale Docker environment variables
$env:DOCKER_HOST = ""
$env:DOCKER_TLS_VERIFY = ""
$env:DOCKER_CERT_PATH = ""

# Run Maven tests
mvn test

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "✓ All tests passed!" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
}
else {
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Red
    Write-Host "✗ Some tests failed" -ForegroundColor Red
    Write-Host "================================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Check the test reports at:" -ForegroundColor Yellow
    Write-Host "  target\surefire-reports\" -ForegroundColor Yellow
}

exit $LASTEXITCODE
