"""
CityFlow ML Pipeline - Main Training Script

Orchestrates the complete ML training pipeline.
"""

import yaml
import logging
import sys
from pathlib import Path
from datetime import datetime

# Add parent directory to path
sys.path.append(str(Path(__file__).parent))

from data_loader import DataLoader
from feature_engineering import TrafficFeatureEngineer, create_target_variable
from model_training import TrafficPredictionModel, CongestionClassificationModel

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('training.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


def load_config(config_path: str = 'config.yaml'):
    """Load configuration from YAML file."""
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)
    return config


def main():
    """Main training pipeline."""
    logger.info("=" * 80)
    logger.info("CityFlow ML Training Pipeline Started")
    logger.info("=" * 80)
    
    # Load configuration
    config = load_config()
    logger.info(f"Configuration loaded from config.yaml")
    
    # Initialize components
    data_loader = DataLoader(config)
    feature_engineer = TrafficFeatureEngineer(config)
    
    try:
        # Step 1: Load training data
        logger.info("\n" + "=" * 80)
        logger.info("Step 1: Loading Training Data")
        logger.info("=" * 80)
        
        train_days = config['training']['train_days']
        
        try:
            # Try to load real data
            df = data_loader.load_training_data(days=train_days)
        except Exception as e:
            logger.warning(f"Could not load real data: {e}")
            logger.info("Generating synthetic data for demonstration...")
            df = data_loader.generate_synthetic_data(
                num_days=train_days,
                num_segments=10,
                freq='5min'
            )
        
        logger.info(f"Data loaded: {df.shape}")
        logger.info(f"Columns: {list(df.columns)}")
        
        # Step 2: Feature Engineering
        logger.info("\n" + "=" * 80)
        logger.info("Step 2: Feature Engineering")
        logger.info("=" * 80)
        
        # Split data: use 80% for historical features, 20% for actual training
        split_idx = int(len(df) * 0.8)
        historical_df = df.iloc[:split_idx].copy()
        training_df = df.iloc[split_idx:].copy()
        
        logger.info(f"Historical data: {historical_df.shape}")
        logger.info(f"Training data: {training_df.shape}")
        
        # Engineer features
        training_df = feature_engineer.prepare_features(
            training_df,
            historical_df=historical_df,
            is_training=True
        )
        
        logger.info(f"Features engineered: {training_df.shape}")
        
        # Step 3: Train Traffic Speed Prediction Models
        logger.info("\n" + "=" * 80)
        logger.info("Step 3: Training Traffic Speed Prediction Models")
        logger.info("=" * 80)
        
        prediction_horizons = config['models']['traffic_speed_prediction']['prediction_horizons']
        
        for horizon in prediction_horizons:
            logger.info(f"\n--- Training model for {horizon}-minute prediction ---")
            
            # Create target variable
            df_with_target = create_target_variable(
                training_df.copy(),
                target_col='speed_kmh',
                prediction_horizon_minutes=horizon
            )
            
            # Drop rows with NaN target
            df_with_target = df_with_target.dropna(subset=[f'speed_kmh_future_{horizon}min'])
            
            logger.info(f"Training samples: {len(df_with_target)}")
            
            # Check minimum samples
            if len(df_with_target) < config['training']['min_samples']:
                logger.warning(f"Not enough samples ({len(df_with_target)}) for {horizon}-min model. Skipping.")
                continue
            
            # Train model
            model_trainer = TrafficPredictionModel(config)
            model, metrics = model_trainer.train_and_log(df_with_target, prediction_horizon=horizon)
            
            # Save model
            model_path = f"models/traffic_pred_{horizon}min.pkl"
            Path("models").mkdir(exist_ok=True)
            model_trainer.save_model(model_path)
            logger.info(f"Model saved to {model_path}")
            
            logger.info(f"✅ {horizon}-minute prediction model trained successfully")
        
        # Step 4: Train Congestion Classification Model (optional)
        if 'congestion_level' in training_df.columns:
            logger.info("\n" + "=" * 80)
            logger.info("Step 4: Training Congestion Classification Model")
            logger.info("=" * 80)
            
            congestion_trainer = CongestionClassificationModel(config)
            model, metrics = congestion_trainer.train_and_log(training_df)
            
            logger.info("✅ Congestion classification model trained successfully")
        
        # Step 5: Summary
        logger.info("\n" + "=" * 80)
        logger.info("Training Pipeline Completed Successfully!")
        logger.info("=" * 80)
        logger.info(f"Models trained: {len(prediction_horizons)}")
        logger.info(f"Timestamp: {datetime.now()}")
        logger.info("\nNext steps:")
        logger.info("1. Check MLflow UI at http://localhost:5001")
        logger.info("2. Start the model serving API: python model_serving_api.py")
        logger.info("3. Test predictions via REST API")
        
    except Exception as e:
        logger.error(f"Training pipeline failed: {e}", exc_info=True)
        raise
    
    finally:
        # Cleanup
        data_loader.close_connections()
        logger.info("\nConnections closed. Exiting.")


if __name__ == "__main__":
    main()
