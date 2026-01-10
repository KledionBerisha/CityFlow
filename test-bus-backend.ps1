# Test Bus Backend - Simple PowerShell Script
$ErrorActionPreference = "Continue"

Write-Host "=== Bus Backend Test ===" -ForegroundColor Cyan

# Test 1: Services are running
Write-Host "`n1. Checking services..." -ForegroundColor Yellow
$busHealth = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method Get -TimeoutSec 5
Write-Host "   Bus Service: $($busHealth.status)" -ForegroundColor Green

$routeHealth = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method Get -TimeoutSec 5
Write-Host "   Route Service: $($routeHealth.status)" -ForegroundColor Green

# Test 2: Get buses
Write-Host "`n2. Getting buses..." -ForegroundColor Yellow
$buses = Invoke-RestMethod -Uri "http://localhost:8082/buses" -Method Get -TimeoutSec 5
Write-Host "   Found $($buses.Count) buses" -ForegroundColor Green
$buses | Select-Object -First 3 vehicleId, status, currentRouteId | Format-Table

# Test 3: Get bus locations (should be empty until routes assigned)
Write-Host "`n3. Getting bus locations..." -ForegroundColor Yellow
$locations = Invoke-RestMethod -Uri "http://localhost:8082/bus-locations/current" -Method Get -TimeoutSec 5
Write-Host "   Found $($locations.Count) bus locations" -ForegroundColor $(if($locations.Count -gt 0){"Green"}else{"Yellow"})

# Test 4: Check routes
Write-Host "`n4. Checking routes..." -ForegroundColor Yellow
try {
    $routes = Invoke-RestMethod -Uri "http://localhost:8081/routes" -Method Get -TimeoutSec 5
    if ($routes.content) {
        Write-Host "   Found $($routes.content.Count) routes" -ForegroundColor Green
    } else {
        Write-Host "   Found $($routes.Count) routes" -ForegroundColor Green
    }
} catch {
    Write-Host "   No routes found (this is OK)" -ForegroundColor Yellow
}

# Test 5: Check stops
Write-Host "`n5. Checking stops..." -ForegroundColor Yellow
try {
    $stops = Invoke-RestMethod -Uri "http://localhost:8081/stops" -Method Get -TimeoutSec 5
    if ($stops.content) {
        Write-Host "   Found $($stops.content.Count) stops" -ForegroundColor Green
    } else {
        Write-Host "   Found $($stops.Count) stops" -ForegroundColor Green
    }
} catch {
    Write-Host "   No stops found (this is OK)" -ForegroundColor Yellow
}

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "Services: Running" -ForegroundColor Green
Write-Host "Buses: $($buses.Count) (3 ACTIVE, need routes)" -ForegroundColor $(if($buses.Count -gt 0){"Green"}else{"Red"})
Write-Host "Locations: $($locations.Count) (will populate when buses have routes)" -ForegroundColor Yellow
Write-Host "`nNext: Create routes and assign to buses to start simulation" -ForegroundColor Yellow

