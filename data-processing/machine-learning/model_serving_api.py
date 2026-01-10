"""
CityFlow ML Pipeline - Model Serving API

FastAPI service for real-time traffic predictions.
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict
import logging
import yaml
import joblib
import pandas as pd
import numpy as np
from datetime import datetime
from pathlib import Path
import sys

# Add parent directory to path
sys.path.append(str(Path(__file__).parent))

from data_loader import DataLoader
from feature_engineering import TrafficFeatureEngineer
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from fastapi.responses import Response

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Load configuration
with open('config.yaml', 'r') as f:
    config = yaml.safe_load(f)

# Initialize FastAPI app
app = FastAPI(
    title="CityFlow Traffic Prediction API",
    description="Real-time traffic speed prediction service",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus metrics (use try-except to avoid duplication on reload)
try:
    prediction_counter = Counter('predictions_total', 'Total number of predictions made')
    prediction_duration = Histogram('prediction_duration_seconds', 'Time spent making predictions')
    api_requests = Counter('api_requests_total', 'Total API requests', ['endpoint', 'method'])
except ValueError:
    # Metrics already registered (happens on reload)
    from prometheus_client import REGISTRY
    prediction_counter = REGISTRY._names_to_collectors.get('predictions_total')
    prediction_duration = REGISTRY._names_to_collectors.get('prediction_duration_seconds')
    api_requests = REGISTRY._names_to_collectors.get('api_requests_total')

# Global model cache
models = {}
feature_engineer = None
data_loader = None


# Pydantic models
class TrafficReading(BaseModel):
    """Single traffic reading for prediction."""
    road_segment_id: str
    timestamp: datetime
    speed_kmh: float = Field(..., gt=0, lt=200)
    vehicle_count: Optional[int] = Field(None, ge=0)
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class PredictionRequest(BaseModel):
    """Prediction request with current traffic readings."""
    readings: List[TrafficReading]
    prediction_horizons: List[int] = Field(default=[10, 20, 30], description="Minutes ahead to predict")


class SpeedPrediction(BaseModel):
    """Single speed prediction result."""
    road_segment_id: str
    current_speed_kmh: float
    predicted_speed_kmh: float
    prediction_horizon_minutes: int
    confidence: Optional[float] = None
    timestamp: datetime


class PredictionResponse(BaseModel):
    """Prediction response."""
    predictions: List[SpeedPrediction]
    model_version: str
    timestamp: datetime


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    models_loaded: int
    timestamp: datetime


@app.on_event("startup")
async def startup_event():
    """Load models on startup."""
    global models, feature_engineer, data_loader
    
    logger.info("Starting CityFlow ML API...")
    
    # Initialize components
    data_loader = DataLoader(config)
    feature_engineer = TrafficFeatureEngineer(config)
    
    # Load models
    models_dir = Path("models")
    if models_dir.exists():
        for model_path in models_dir.glob("traffic_pred_*.pkl"):
            try:
                horizon = int(model_path.stem.split('_')[-1].replace('min', ''))
                model_data = joblib.load(model_path)
                models[horizon] = model_data
                logger.info(f"Loaded model for {horizon}-minute prediction")
            except Exception as e:
                logger.error(f"Failed to load model {model_path}: {e}")
    
    if not models:
        logger.warning("No models loaded. Train models first using train_models.py")
    else:
        logger.info(f"✅ {len(models)} models loaded successfully")


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    api_requests.labels(endpoint='/health', method='GET').inc()
    
    return HealthResponse(
        status="healthy" if models else "no_models",
        models_loaded=len(models),
        timestamp=datetime.now()
    )


@app.get("/metrics")
async def metrics():
    """Prometheus metrics endpoint."""
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.get("/models")
async def list_models():
    """List available models."""
    api_requests.labels(endpoint='/models', method='GET').inc()
    
    return {
        "models": [
            {
                "horizon_minutes": horizon,
                "features": len(data['feature_columns']),
                "model_type": data['config']['models']['traffic_speed_prediction']['type']
            }
            for horizon, data in models.items()
        ]
    }


@app.post("/predict", response_model=PredictionResponse)
@prediction_duration.time()
async def predict_traffic(request: PredictionRequest):
    """
    Make traffic speed predictions.
    
    Args:
        request: Prediction request with current traffic readings
        
    Returns:
        Predictions for requested horizons
    """
    api_requests.labels(endpoint='/predict', method='POST').inc()
    
    if not models:
        raise HTTPException(status_code=503, detail="No models available. Train models first.")
    
    try:
        # Convert readings to DataFrame
        readings_data = [r.dict() for r in request.readings]
        df = pd.DataFrame(readings_data)
        
        logger.info(f"Received {len(df)} readings for prediction")
        
        # Load historical data for feature engineering
        try:
            historical_df = data_loader.load_training_data(days=7)
        except:
            logger.warning("Could not load historical data, using synthetic")
            historical_df = data_loader.generate_synthetic_data(num_days=7)
        
        # Engineer features
        df_features = feature_engineer.prepare_features(
            df,
            historical_df=historical_df,
            is_training=False
        )
        
        # Make predictions for each horizon
        all_predictions = []
        
        for horizon in request.prediction_horizons:
            if horizon not in models:
                logger.warning(f"No model available for {horizon}-minute prediction")
                continue
            
            model_data = models[horizon]
            model = model_data['model']
            feature_columns = model_data['feature_columns']
            
            # Prepare features
            X = df_features[feature_columns].fillna(0)
            
            # Make predictions
            predictions = model.predict(X)
            prediction_counter.inc(len(predictions))
            
            # Create prediction objects
            for idx, (_, row) in enumerate(df.iterrows()):
                all_predictions.append(
                    SpeedPrediction(
                        road_segment_id=row['road_segment_id'],
                        current_speed_kmh=row['speed_kmh'],
                        predicted_speed_kmh=float(predictions[idx]),
                        prediction_horizon_minutes=horizon,
                        confidence=None,  # Could add prediction intervals here
                        timestamp=datetime.now()
                    )
                )
        
        return PredictionResponse(
            predictions=all_predictions,
            model_version="1.0.0",
            timestamp=datetime.now()
        )
    
    except Exception as e:
        logger.error(f"Prediction failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.post("/predict/batch")
async def predict_batch(road_segment_ids: List[str], prediction_horizons: List[int] = [10, 20, 30]):
    """
    Make predictions for specified road segments using recent data.
    
    Args:
        road_segment_ids: List of road segment IDs to predict
        prediction_horizons: Minutes ahead to predict
        
    Returns:
        Predictions for all segments
    """
    api_requests.labels(endpoint='/predict/batch', method='POST').inc()
    
    try:
        # Load recent data
        df = data_loader.load_prediction_data(minutes=60)
        
        # Filter to requested segments
        df = df[df['road_segment_id'].isin(road_segment_ids)]
        
        if df.empty:
            raise HTTPException(status_code=404, detail="No data found for requested segments")
        
        # Get latest reading per segment
        df = df.sort_values('timestamp').groupby('road_segment_id').tail(1)
        
        # Convert to prediction request format
        readings = [
            TrafficReading(
                road_segment_id=row['road_segment_id'],
                timestamp=row['timestamp'],
                speed_kmh=row['speed_kmh'],
                vehicle_count=row.get('vehicle_count'),
                latitude=row.get('latitude'),
                longitude=row.get('longitude')
            )
            for _, row in df.iterrows()
        ]
        
        # Make predictions
        request = PredictionRequest(
            readings=readings,
            prediction_horizons=prediction_horizons
        )
        
        return await predict_traffic(request)
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Batch prediction failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Batch prediction failed: {str(e)}")


@app.get("/predict/all")
async def predict_all_segments(horizon: int = 30):
    """
    Predict traffic for all road segments.
    
    Args:
        horizon: Prediction horizon in minutes
        
    Returns:
        Predictions for all segments
    """
    api_requests.labels(endpoint='/predict/all', method='GET').inc()
    
    # Check if models are loaded
    if not models:
        logger.warning("No models loaded - returning empty predictions")
        return PredictionResponse(
            predictions=[],
            model_version="no_models",
            timestamp=datetime.now()
        )
    
    try:
        # Load recent data
        df = data_loader.load_prediction_data(minutes=60)
        
        if df.empty:
            logger.info("No recent data available - returning empty predictions")
            return PredictionResponse(
                predictions=[],
                model_version=models.get(horizon, {}).get('version', 'unknown'),
                timestamp=datetime.now()
            )
        
        # Get latest reading per segment
        df = df.sort_values('timestamp').groupby('road_segment_id').tail(1)
        
        # Get all segment IDs
        segment_ids = df['road_segment_id'].unique().tolist()
        
        return await predict_batch(segment_ids, [horizon])
    
    except Exception as e:
        logger.error(f"All segments prediction failed: {e}", exc_info=True)
        # Return empty predictions instead of error
        return PredictionResponse(
            predictions=[],
            model_version="error",
            timestamp=datetime.now()
        )


if __name__ == "__main__":
    import uvicorn
    
    host = config['serving']['api']['host']
    port = config['serving']['api']['port']
    workers = config['serving']['api']['workers']
    
    logger.info(f"Starting API server on {host}:{port}")
    
    uvicorn.run(
        "model_serving_api:app",
        host=host,
        port=port,
        workers=workers,
        log_level=config['serving']['api']['log_level']
    )
