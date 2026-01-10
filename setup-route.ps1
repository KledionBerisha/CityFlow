# Quick Route Setup
Write-Host "Setting up route and assigning buses..." -ForegroundColor Cyan

# Create route
$route = @{
    code = "R1"
    name = "Route 1 - Downtown Loop"
    active = $true
} | ConvertTo-Json

try {
    $routeResponse = Invoke-RestMethod -Uri "http://localhost:8081/routes" -Method Post -Body $route -ContentType "application/json"
    $routeId = $routeResponse.id
    Write-Host "Created route: $routeId" -ForegroundColor Green
} catch {
    Write-Host "Route might already exist, getting existing..." -ForegroundColor Yellow
    $routes = Invoke-RestMethod -Uri "http://localhost:8081/routes"
    if ($routes.content) { $routeId = $routes.content[0].id } else { $routeId = $routes[0].id }
    Write-Host "Using route: $routeId" -ForegroundColor Green
}

# Get existing stops or create new ones
$stops = Invoke-RestMethod -Uri "http://localhost:8081/stops"
if ($stops.content) { $existingStops = $stops.content } else { $existingStops = $stops }

# Create additional stops if needed
$stopData = @(
    @{name="Stop A"; lat=42.0; lon=21.0},
    @{name="Stop B"; lat=42.01; lon=21.01},
    @{name="Stop C"; lat=42.02; lon=21.02}
)

$allStops = @()
foreach ($s in $existingStops) {
    $allStops += $s
}

# Create missing stops
for ($i = $allStops.Count; $i -lt 3; $i++) {
    $newStop = @{
        name = $stopData[$i].name
        latitude = $stopData[$i].lat
        longitude = $stopData[$i].lon
        terminal = ($i -eq 0)
        active = $true
    } | ConvertTo-Json
    
    try {
        $stopResponse = Invoke-RestMethod -Uri "http://localhost:8081/stops" -Method Post -Body $newStop -ContentType "application/json"
        $allStops += $stopResponse
        Write-Host "Created stop: $($stopResponse.name)" -ForegroundColor Green
    } catch {
        Write-Host "Stop creation failed (might exist): $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Assign stops to route
if ($allStops.Count -ge 3) {
    $routeStops = @()
    for ($i = 0; $i -lt [Math]::Min(3, $allStops.Count); $i++) {
        $routeStops += @{
            stopId = $allStops[$i].id
            sequenceOrder = $i + 1
        }
    }
    
    try {
        $assigned = Invoke-RestMethod -Uri "http://localhost:8081/routes/$routeId/stops" -Method Put -Body ($routeStops | ConvertTo-Json) -ContentType "application/json"
        Write-Host "Assigned $($assigned.Count) stops to route" -ForegroundColor Green
    } catch {
        Write-Host "Error assigning stops: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Get buses and show info
$buses = Invoke-RestMethod -Uri "http://localhost:8082/buses"
$activeBuses = $buses | Where-Object { $_.status -eq "ACTIVE" -and -not $_.currentRouteId }

Write-Host "`nRoute ID: $routeId" -ForegroundColor Cyan
Write-Host "Active buses without routes: $($activeBuses.Count)" -ForegroundColor Yellow
Write-Host "`nTo assign buses, you need to update them with currentRouteId" -ForegroundColor Yellow
Write-Host "The bus service doesn't have a direct update endpoint for routeId" -ForegroundColor Yellow
Write-Host "You may need to use the bus creation endpoint or add an update endpoint" -ForegroundColor Yellow

