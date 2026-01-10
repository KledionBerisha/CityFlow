#!/bin/bash
# CityFlow ML Pipeline - Quick Setup Script

set -e

echo "========================================"
echo "CityFlow ML Pipeline - Quick Setup"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check Python version
echo -e "${YELLOW}Checking Python version...${NC}"
python_version=$(python3 --version 2>&1 | awk '{print $2}')
echo "Python version: $python_version"

if ! python3 -c 'import sys; assert sys.version_info >= (3,10)' 2>/dev/null; then
    echo -e "${RED}Error: Python 3.10+ required${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Python version OK${NC}"
echo ""

# Create virtual environment
echo -e "${YELLOW}Creating virtual environment...${NC}"
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo -e "${GREEN}✓ Virtual environment created${NC}"
else
    echo -e "${GREEN}✓ Virtual environment already exists${NC}"
fi
echo ""

# Activate virtual environment
echo -e "${YELLOW}Activating virtual environment...${NC}"
source venv/bin/activate
echo -e "${GREEN}✓ Virtual environment activated${NC}"
echo ""

# Install dependencies
echo -e "${YELLOW}Installing dependencies...${NC}"
pip install --upgrade pip
pip install -r requirements.txt
echo -e "${GREEN}✓ Dependencies installed${NC}"
echo ""

# Create necessary directories
echo -e "${YELLOW}Creating directories...${NC}"
mkdir -p models
mkdir -p logs
mkdir -p mlruns
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# Check if config exists
if [ ! -f "config.yaml" ]; then
    echo -e "${RED}Error: config.yaml not found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Configuration file found${NC}"
echo ""

# Train models
echo -e "${YELLOW}Do you want to train models now? (y/n)${NC}"
read -r response

if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
    echo ""
    echo -e "${YELLOW}Starting model training...${NC}"
    python train_models.py
    echo ""
    echo -e "${GREEN}✓ Model training complete${NC}"
else
    echo -e "${YELLOW}Skipping model training${NC}"
fi

echo ""
echo "========================================"
echo -e "${GREEN}Setup Complete!${NC}"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. View MLflow UI:    mlflow ui --port 5001"
echo "2. Start API server:  python model_serving_api.py"
echo "3. Start consumer:    python realtime_prediction_consumer.py"
echo ""
echo "Or use Docker:"
echo "  docker-compose up -d"
echo ""
echo "API Documentation: http://localhost:8090/docs"
echo "MLflow UI:         http://localhost:5001"
echo ""
