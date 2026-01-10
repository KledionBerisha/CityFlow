# CityFlow ML Pipeline - Quick Setup Script (PowerShell)
# For Windows users

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CityFlow ML Pipeline - Quick Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Python version
Write-Host "Checking Python version..." -ForegroundColor Yellow
$pythonVersion = python --version 2>&1
Write-Host "Python version: $pythonVersion"

try {
    $version = [version]($pythonVersion -replace "Python ", "")
    if ($version.Major -lt 3 -or ($version.Major -eq 3 -and $version.Minor -lt 10)) {
        Write-Host "Error: Python 3.10+ required" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error checking Python version" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Python version OK" -ForegroundColor Green
Write-Host ""

# Create virtual environment
Write-Host "Creating virtual environment..." -ForegroundColor Yellow
if (-not (Test-Path "venv")) {
    python -m venv venv
    Write-Host "✓ Virtual environment created" -ForegroundColor Green
} else {
    Write-Host "✓ Virtual environment already exists" -ForegroundColor Green
}
Write-Host ""

# Activate virtual environment
Write-Host "Activating virtual environment..." -ForegroundColor Yellow
& ".\venv\Scripts\Activate.ps1"
Write-Host "✓ Virtual environment activated" -ForegroundColor Green
Write-Host ""

# Install dependencies
Write-Host "Installing dependencies..." -ForegroundColor Yellow
pip install --upgrade pip
pip install -r requirements.txt
Write-Host "✓ Dependencies installed" -ForegroundColor Green
Write-Host ""

# Create necessary directories
Write-Host "Creating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "models" | Out-Null
New-Item -ItemType Directory -Force -Path "logs" | Out-Null
New-Item -ItemType Directory -Force -Path "mlruns" | Out-Null
Write-Host "✓ Directories created" -ForegroundColor Green
Write-Host ""

# Check if config exists
if (-not (Test-Path "config.yaml")) {
    Write-Host "Error: config.yaml not found" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Configuration file found" -ForegroundColor Green
Write-Host ""

# Train models
$response = Read-Host "Do you want to train models now? (y/n)"

if ($response -match "^[Yy]") {
    Write-Host ""
    Write-Host "Starting model training..." -ForegroundColor Yellow
    python train_models.py
    Write-Host ""
    Write-Host "✓ Model training complete" -ForegroundColor Green
} else {
    Write-Host "Skipping model training" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. View MLflow UI:    mlflow ui --port 5001"
Write-Host "2. Start API server:  python model_serving_api.py"
Write-Host "3. Start consumer:    python realtime_prediction_consumer.py"
Write-Host ""
Write-Host "Or use Docker:"
Write-Host "  docker-compose up -d"
Write-Host ""
Write-Host "API Documentation: http://localhost:8090/docs"
Write-Host "MLflow UI:         http://localhost:5001"
Write-Host ""
