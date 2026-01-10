# CityFlow - Start Monitoring Stack (PowerShell)

Write-Host "🚀 Starting CityFlow Monitoring Stack..." -ForegroundColor Green

# Start Prometheus and Grafana
Write-Host "📊 Starting Prometheus..." -ForegroundColor Yellow
docker-compose up -d prometheus

Start-Sleep -Seconds 5

Write-Host "📈 Starting Grafana..." -ForegroundColor Yellow
docker-compose up -d grafana

Start-Sleep -Seconds 10

# Check services status
Write-Host "`n✅ Monitoring Stack Status:" -ForegroundColor Green
docker-compose ps prometheus grafana

# Test endpoints
Write-Host "`n🔍 Testing endpoints..." -ForegroundColor Yellow

try {
    $prometheusStatus = Invoke-WebRequest -Uri "http://localhost:9091/-/healthy" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Prometheus: Healthy" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Prometheus: Not responding yet (waiting...)" -ForegroundColor Yellow
}

try {
    $grafanaStatus = Invoke-WebRequest -Uri "http://localhost:3001/api/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Grafana: Healthy" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Grafana: Not responding yet (waiting...)" -ForegroundColor Yellow
}

Write-Host "`n🌐 Access Monitoring:" -ForegroundColor Cyan
Write-Host "  - Prometheus: http://localhost:9091" -ForegroundColor White
Write-Host "  - Grafana: http://localhost:3001 (admin/admin)" -ForegroundColor White
Write-Host "`n📊 View Logs:" -ForegroundColor Cyan
Write-Host "  docker-compose logs -f prometheus grafana" -ForegroundColor White
