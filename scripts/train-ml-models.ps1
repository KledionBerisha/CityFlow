# CityFlow - Train ML Models Script

Write-Host "🤖 Training CityFlow ML Models..." -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

# Navigate to ML directory
Set-Location data-processing\machine-learning

# Check Python
Write-Host "`n🐍 Checking Python..." -ForegroundColor Yellow
python --version

# Install dependencies if needed
Write-Host "`n📦 Installing dependencies..." -ForegroundColor Yellow
pip install -q xgboost scikit-learn pandas numpy joblib pyyaml 2>&1 | Out-Null

# Train models
Write-Host "`n🎓 Training models..." -ForegroundColor Yellow
python train_simple.py

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Models trained successfully!" -ForegroundColor Green
    
    # Check models exist
    $models = Get-ChildItem -Path "models" -Filter "traffic_pred_*.pkl"
    Write-Host "`n📁 Models found:" -ForegroundColor Cyan
    foreach ($model in $models) {
        $sizeKB = [math]::Round($model.Length / 1KB, 1)
        Write-Host "  - $($model.Name) ($sizeKB KB)" -ForegroundColor White
    }
    
    Write-Host "`n🔄 Restarting ML API to load models..." -ForegroundColor Yellow
    Set-Location ..\..\..
    docker-compose restart ml-api
    
    Start-Sleep -Seconds 10
    
    # Verify models loaded
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:8090/health" -UseBasicParsing | ConvertFrom-Json
        Write-Host "✅ ML API Health: $($health.status)" -ForegroundColor Green
        Write-Host "✅ Models Loaded: $($health.models_loaded)" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ Could not verify ML API health" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n❌ Model training failed!" -ForegroundColor Red
    Set-Location ..\..\..
    exit 1
}

Set-Location ..\..\..
Write-Host "`n✅ Done!" -ForegroundColor Green
