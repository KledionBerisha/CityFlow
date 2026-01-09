# Assign buses to route
$routeId = "3835c6c0-0727-413c-b99a-811b06c29dcc"  # From previous setup
$buses = Invoke-RestMethod -Uri "http://localhost:8082/buses"

Write-Host "Assigning buses to route $routeId..." -ForegroundColor Cyan

foreach ($bus in $buses) {
    if ($bus.status -eq "ACTIVE" -and -not $bus.currentRouteId) {
        try {
            $updated = Invoke-RestMethod -Uri "http://localhost:8082/buses/$($bus.id)/route?routeId=$routeId" -Method Put -TimeoutSec 10
            Write-Host "  [OK] Assigned $($bus.vehicleId) to route" -ForegroundColor Green
        } catch {
            Write-Host "  [ERROR] Failed to assign $($bus.vehicleId): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`nWaiting 10 seconds for simulation to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check locations
$locations = Invoke-RestMethod -Uri "http://localhost:8082/bus-locations/current"
Write-Host "`nBus locations: $($locations.Count)" -ForegroundColor $(if($locations.Count -gt 0){"Green"}else{"Yellow"})
if ($locations.Count -gt 0) {
    $locations | Select-Object -First 3 vehicleId, latitude, longitude, speedKmh, nextStopId | Format-Table
}

