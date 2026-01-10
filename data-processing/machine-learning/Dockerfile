# Dockerfile for CityFlow ML Services

FROM python:3.10-slim

LABEL maintainer="CityFlow Team"
LABEL description="Machine Learning services for traffic prediction"

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY *.py ./
COPY config.yaml ./

# Create directories
RUN mkdir -p models logs mlruns

# Expose ports
EXPOSE 8090 5001

# Environment variables
ENV PYTHONUNBUFFERED=1
ENV MLFLOW_TRACKING_URI=http://mlflow:5001

# Default command (can be overridden)
CMD ["python", "model_serving_api.py"]
