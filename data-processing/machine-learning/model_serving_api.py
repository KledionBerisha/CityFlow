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


class CongestionDurationRequest(BaseModel):
    """Request for congestion duration prediction."""
    road_segment_id: str
    current_speed_kmh: float = Field(..., gt=0, lt=200)
    normal_speed_kmh: float = Field(default=50, gt=0, lt=200, description="Normal/free-flow speed for this road")
    current_congestion_level: float = Field(..., ge=0, le=1, description="Current congestion 0-1")
    vehicle_count: Optional[int] = Field(None, ge=0)
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class CongestionDurationResponse(BaseModel):
    """Response for congestion duration prediction."""
    road_segment_id: str
    current_congestion_level: float
    predicted_duration_minutes: int = Field(description="Estimated minutes until congestion clears")
    confidence: str = Field(description="low, medium, or high confidence")
    expected_clear_time: datetime
    prediction_factors: Dict
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
            
            # Extract model and feature columns (handle both dict and direct model formats)
            if isinstance(model_data, dict):
                model = model_data.get('model', model_data)
                model_feature_cols = model_data.get('feature_columns', [])
            else:
                # Legacy format: model is directly stored
                model = model_data
                model_feature_cols = feature_columns
            
            if not model_feature_cols:
                # Fallback: try to get from df_features
                logger.warning(f"No feature columns found in model data for {horizon}min, using all numeric columns")
                model_feature_cols = df_features.select_dtypes(include=[np.number]).columns.tolist()
            
            # Ensure all required features exist in df_features
            missing_cols = [col for col in model_feature_cols if col not in df_features.columns]
            if missing_cols:
                logger.warning(f"Missing features {missing_cols} for {horizon}min model, filling with 0")
                for col in missing_cols:
                    df_features[col] = 0
            
            # Select and order features to match training order
            X = df_features[model_feature_cols].fillna(0)
            
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
    Predict traffic for all road segments using current traffic readings.
    
    Args:
        horizon: Prediction horizon in minutes
        
    Returns:
        Predictions for all segments with current data
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
        # Load recent traffic readings (last 60 minutes)
        df = data_loader.load_prediction_data(minutes=60)
        
        if df.empty:
            logger.info("No recent data available - returning empty predictions")
            return PredictionResponse(
                predictions=[],
                model_version=models.get(horizon, {}).get('version', 'unknown'),
                timestamp=datetime.now()
            )
        
        # Get latest reading per segment (most recent data)
        df = df.sort_values('timestamp', ascending=False).groupby('road_segment_id').first().reset_index()
        
        # Convert to prediction request format
        readings = []
        for _, row in df.iterrows():
            # Ensure we have required fields
            road_segment_id = row.get('road_segment_id', f"SEGMENT-{row.get('sensorId', '001')}")
            speed_kmh = float(row.get('speed_kmh', row.get('averageSpeed', 40.0)))
            vehicle_count = int(row.get('vehicle_count', row.get('vehicleCount', 0))) if pd.notna(row.get('vehicle_count', row.get('vehicleCount', 0))) else None
            latitude = float(row.get('latitude', 42.6629)) if pd.notna(row.get('latitude')) else 42.6629
            longitude = float(row.get('longitude', 21.1655)) if pd.notna(row.get('longitude')) else 21.1655
            
            # Get timestamp
            ts = row.get('timestamp', datetime.now())
            if isinstance(ts, str):
                ts = pd.to_datetime(ts)
            if pd.isna(ts):
                ts = datetime.now()
            
            readings.append(TrafficReading(
                road_segment_id=road_segment_id,
                timestamp=ts if isinstance(ts, datetime) else pd.to_datetime(ts),
                speed_kmh=speed_kmh,
                vehicle_count=vehicle_count,
                latitude=latitude,
                longitude=longitude
            ))
        
        if not readings:
            logger.warning("No valid readings to predict")
            return PredictionResponse(
                predictions=[],
                model_version=models.get(horizon, {}).get('version', 'unknown'),
                timestamp=datetime.now()
            )
        
        # Create prediction request
        request = PredictionRequest(
            readings=readings,
            prediction_horizons=[horizon]
        )
        
        # Make predictions
        return await predict_traffic(request)
    
    except Exception as e:
        logger.error(f"All segments prediction failed: {e}", exc_info=True)
        # Return empty predictions instead of error
        return PredictionResponse(
            predictions=[],
            model_version="error",
            timestamp=datetime.now()
        )


@app.get("/predict/congestion-hotspots")
async def get_congestion_hotspots(horizon: int = 30):
    """
    Get biggest congestion hotspots with predictions and duration estimates.
    Returns segments with worst congestion sorted by severity, including duration predictions.
    """
    api_requests.labels(endpoint='/predict/congestion-hotspots', method='GET').inc()
    
    try:
        # Get predictions for all segments
        predictions_response = await predict_all_segments(horizon)
        
        if not predictions_response or not predictions_response.predictions:
            logger.warning("No predictions available for hotspots")
            return []
        
        # Load current traffic data to get locations and congestion levels
        df = data_loader.load_prediction_data(minutes=10)
        
        # Calculate congestion scores and get duration predictions
        hotspots = []
        normal_speed = 50.0  # Assume normal free-flow speed
        
        for pred in predictions_response.predictions:
            current_speed = pred.current_speed_kmh
            predicted_speed = pred.predicted_speed_kmh
            
            # Calculate congestion severity (0-1, where 1 is worst)
            current_congestion_level = max(0, 1 - (current_speed / normal_speed))
            predicted_congestion_level = max(0, 1 - (predicted_speed / normal_speed))
            congestion_severity = max(current_congestion_level, predicted_congestion_level)
            
            # Get road segment data for location
            segment_data = df[df.get('road_segment_id', '') == pred.road_segment_id] if not df.empty else pd.DataFrame()
            if not segment_data.empty:
                row = segment_data.iloc[0]
                latitude = float(row.get('latitude', 42.6629)) if pd.notna(row.get('latitude')) else 42.6629
                longitude = float(row.get('longitude', 21.1655)) if pd.notna(row.get('longitude')) else 21.1655
            else:
                # Default location (Prishtina center)
                latitude = 42.6629
                longitude = 21.1655
            
            # Get congestion duration prediction
            duration_prediction = None
            try:
                duration_request = CongestionDurationRequest(
                    road_segment_id=pred.road_segment_id,
                    current_speed_kmh=current_speed,
                    normal_speed_kmh=normal_speed,
                    current_congestion_level=current_congestion_level,
                    vehicle_count=None,
                    latitude=latitude,
                    longitude=longitude
                )
                duration_response = await predict_congestion_duration(duration_request)
                duration_prediction = {
                    "predicted_duration_minutes": duration_response.predicted_duration_minutes,
                    "expected_clear_time": duration_response.expected_clear_time.isoformat() if hasattr(duration_response.expected_clear_time, 'isoformat') else str(duration_response.expected_clear_time),
                    "confidence": duration_response.confidence
                }
            except Exception as e:
                logger.warning(f"Could not get duration prediction for {pred.road_segment_id}: {e}")
            
            hotspots.append({
                "road_segment_id": pred.road_segment_id,
                "latitude": latitude,
                "longitude": longitude,
                "current_speed_kmh": current_speed,
                "predicted_speed_kmh": predicted_speed,
                "current_congestion_level": round(current_congestion_level, 3),
                "predicted_congestion_level": round(predicted_congestion_level, 3),
                "congestion_severity": round(congestion_severity, 3),
                "prediction_horizon_minutes": pred.prediction_horizon_minutes,
                "duration_prediction": duration_prediction,
                "timestamp": pred.timestamp.isoformat() if hasattr(pred.timestamp, 'isoformat') else str(pred.timestamp)
            })
        
        # Sort by congestion severity (worst first) - biggest congestions first
        hotspots.sort(key=lambda x: x['congestion_severity'], reverse=True)
        
        return hotspots[:20]  # Return top 20 worst congestions
    
    except Exception as e:
        logger.error(f"Congestion hotspots failed: {e}", exc_info=True)
        return []


@app.post("/predict/congestion-duration", response_model=CongestionDurationResponse)
async def predict_congestion_duration(request: CongestionDurationRequest):
    """
    Predict how long current congestion will last.
    
    Uses a combination of:
    - Current congestion level
    - Time of day (peak hours vs off-peak)
    - Historical patterns
    - Vehicle density
    
    Returns estimated minutes until traffic returns to normal flow.
    """
    api_requests.labels(endpoint='/predict/congestion-duration', method='POST').inc()
    
    try:
        now = datetime.now()
        current_hour = now.hour
        
        # Calculate congestion severity (0-1 scale)
        speed_ratio = request.current_speed_kmh / request.normal_speed_kmh
        congestion_severity = max(0, 1 - speed_ratio)
        
        # Factors affecting duration
        factors = {}
        
        # Time of day factor
        is_morning_peak = 7 <= current_hour <= 9
        is_evening_peak = 16 <= current_hour <= 19
        is_peak_hour = is_morning_peak or is_evening_peak
        
        if is_peak_hour:
            time_factor = 1.5  # Congestion lasts longer during peak
            if is_morning_peak:
                # Morning rush typically clears by 9:30
                minutes_to_peak_end = max(0, (9 * 60 + 30) - (current_hour * 60 + now.minute))
            else:
                # Evening rush typically clears by 19:30
                minutes_to_peak_end = max(0, (19 * 60 + 30) - (current_hour * 60 + now.minute))
            factors['peak_hour'] = True
            factors['minutes_to_peak_end'] = minutes_to_peak_end
        else:
            time_factor = 0.7  # Congestion clears faster outside peak
            minutes_to_peak_end = None
            factors['peak_hour'] = False
        
        # Day of week factor
        day_of_week = now.weekday()
        is_weekend = day_of_week >= 5
        weekend_factor = 0.6 if is_weekend else 1.0
        factors['weekend'] = is_weekend
        
        # Base duration calculation (in minutes)
        # Light congestion (0.1-0.3): 5-15 min
        # Moderate congestion (0.3-0.6): 15-30 min
        # Heavy congestion (0.6-0.8): 30-60 min
        # Severe congestion (0.8-1.0): 45-90 min
        
        if congestion_severity < 0.3:
            base_duration = 5 + congestion_severity * 33  # 5-15 min
            confidence = "high"
        elif congestion_severity < 0.6:
            base_duration = 15 + (congestion_severity - 0.3) * 50  # 15-30 min
            confidence = "medium"
        elif congestion_severity < 0.8:
            base_duration = 30 + (congestion_severity - 0.6) * 150  # 30-60 min
            confidence = "medium"
        else:
            base_duration = 45 + (congestion_severity - 0.8) * 225  # 45-90 min
            confidence = "low"
        
        # Apply factors
        predicted_duration = base_duration * time_factor * weekend_factor
        
        # Cap by peak end time if applicable
        if is_peak_hour and minutes_to_peak_end is not None:
            predicted_duration = min(predicted_duration, minutes_to_peak_end + 10)
        
        # Minimum 5 minutes, maximum 120 minutes
        predicted_duration = max(5, min(120, int(predicted_duration)))
        
        # Calculate expected clear time
        from datetime import timedelta
        expected_clear_time = now + timedelta(minutes=predicted_duration)
        
        factors['congestion_severity'] = round(congestion_severity, 2)
        factors['time_factor'] = time_factor
        factors['weekend_factor'] = weekend_factor
        
        logger.info(f"Congestion duration prediction for {request.road_segment_id}: "
                   f"{predicted_duration} minutes (severity: {congestion_severity:.2f})")
        
        return CongestionDurationResponse(
            road_segment_id=request.road_segment_id,
            current_congestion_level=request.current_congestion_level,
            predicted_duration_minutes=predicted_duration,
            confidence=confidence,
            expected_clear_time=expected_clear_time,
            prediction_factors=factors,
            timestamp=now
        )
    
    except Exception as e:
        logger.error(f"Congestion duration prediction failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


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
