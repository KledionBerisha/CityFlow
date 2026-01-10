"""
CityFlow ML Pipeline - Real-time Prediction Consumer

Kafka consumer that makes real-time traffic predictions and publishes results.
"""

import json
import logging
import yaml
import joblib
import pandas as pd
import numpy as np
from datetime import datetime
from pathlib import Path
from confluent_kafka import Consumer, Producer, KafkaError
from confluent_kafka.avro import AvroConsumer, AvroProducer
from confluent_kafka.avro.serializer import SerializerError
import sys

# Add parent directory to path
sys.path.append(str(Path(__file__).parent))

from data_loader import DataLoader
from feature_engineering import TrafficFeatureEngineer

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class RealTimePredictionService:
    """Real-time traffic prediction service using Kafka."""
    
    def __init__(self, config_path: str = 'config.yaml'):
        """Initialize the prediction service."""
        # Load configuration
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)
        
        # Initialize components
        self.data_loader = DataLoader(self.config)
        self.feature_engineer = TrafficFeatureEngineer(self.config)
        self.models = {}
        self.historical_cache = None
        
        # Load models
        self._load_models()
        
        # Setup Kafka consumer
        self.consumer = self._create_consumer()
        
        # Setup Kafka producer for predictions
        self.producer = self._create_producer()
        
        logger.info("Real-time prediction service initialized")
    
    def _load_models(self):
        """Load all trained models."""
        models_dir = Path("models")
        
        if not models_dir.exists():
            logger.error("Models directory not found. Train models first.")
            sys.exit(1)
        
        for model_path in models_dir.glob("traffic_pred_*.pkl"):
            try:
                horizon = int(model_path.stem.split('_')[-1].replace('min', ''))
                model_data = joblib.load(model_path)
                self.models[horizon] = model_data
                logger.info(f"Loaded model for {horizon}-minute prediction")
            except Exception as e:
                logger.error(f"Failed to load model {model_path}: {e}")
        
        if not self.models:
            logger.error("No models loaded")
            sys.exit(1)
        
        logger.info(f"✅ {len(self.models)} models loaded")
    
    def _create_consumer(self):
        """Create Kafka consumer."""
        kafka_config = self.config['kafka']
        
        consumer_config = {
            'bootstrap.servers': kafka_config['bootstrap_servers'],
            'group.id': kafka_config['consumer_group'],
            'auto.offset.reset': 'latest',
            'enable.auto.commit': True
        }
        
        consumer = Consumer(consumer_config)
        topic = kafka_config['topics']['traffic_input']
        consumer.subscribe([topic])
        
        logger.info(f"Kafka consumer created, subscribed to: {topic}")
        return consumer
    
    def _create_producer(self):
        """Create Kafka producer."""
        kafka_config = self.config['kafka']
        
        producer_config = {
            'bootstrap.servers': kafka_config['bootstrap_servers'],
            'client.id': 'ml-prediction-service'
        }
        
        producer = Producer(producer_config)
        logger.info("Kafka producer created")
        return producer
    
    def _load_historical_data(self, force_refresh: bool = False):
        """Load historical data for feature engineering."""
        if self.historical_cache is None or force_refresh:
            logger.info("Loading historical data...")
            try:
                self.historical_cache = self.data_loader.load_training_data(days=7)
            except:
                logger.warning("Could not load historical data, using synthetic")
                self.historical_cache = self.data_loader.generate_synthetic_data(num_days=7)
            
            logger.info(f"Historical data cached: {self.historical_cache.shape}")
    
    def process_reading(self, reading: dict) -> dict:
        """
        Process a single traffic reading and make predictions.
        
        Args:
            reading: Traffic reading from Kafka
            
        Returns:
            Prediction results
        """
        try:
            # Convert to DataFrame
            df = pd.DataFrame([reading])
            
            # Ensure historical data is loaded
            self._load_historical_data()
            
            # Engineer features
            df_features = self.feature_engineer.prepare_features(
                df,
                historical_df=self.historical_cache,
                is_training=False
            )
            
            # Make predictions for all horizons
            predictions = {}
            
            for horizon, model_data in self.models.items():
                model = model_data['model']
                feature_columns = model_data['feature_columns']
                
                # Prepare features
                X = df_features[feature_columns].fillna(0)
                
                # Predict
                pred = model.predict(X)[0]
                predictions[f"{horizon}min"] = float(pred)
            
            # Create prediction result
            result = {
                'road_segment_id': reading.get('road_segment_id', 'UNKNOWN'),
                'timestamp': datetime.now().isoformat(),
                'current_speed_kmh': reading.get('speed_kmh', reading.get('avg_speed', 0)),
                'predictions': predictions,
                'model_version': '1.0.0'
            }
            
            return result
        
        except Exception as e:
            logger.error(f"Error processing reading: {e}", exc_info=True)
            return None
    
    def publish_prediction(self, prediction: dict):
        """Publish prediction to Kafka."""
        try:
            topic = self.config['kafka']['topics']['prediction_output']
            
            # Convert to JSON
            value = json.dumps(prediction).encode('utf-8')
            
            # Produce message
            self.producer.produce(
                topic,
                value=value,
                callback=self._delivery_callback
            )
            
            # Flush to ensure delivery
            self.producer.poll(0)
        
        except Exception as e:
            logger.error(f"Error publishing prediction: {e}")
    
    def _delivery_callback(self, err, msg):
        """Callback for message delivery."""
        if err:
            logger.error(f"Message delivery failed: {err}")
        else:
            logger.debug(f"Message delivered to {msg.topic()} [{msg.partition()}]")
    
    def run(self):
        """Run the prediction service."""
        logger.info("Starting real-time prediction service...")
        logger.info("Waiting for traffic readings...")
        
        message_count = 0
        
        try:
            while True:
                # Poll for messages
                msg = self.consumer.poll(timeout=1.0)
                
                if msg is None:
                    continue
                
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        logger.debug(f"Reached end of partition {msg.partition()}")
                    else:
                        logger.error(f"Consumer error: {msg.error()}")
                    continue
                
                # Parse message
                try:
                    reading = json.loads(msg.value().decode('utf-8'))
                    message_count += 1
                    
                    logger.info(f"Received reading #{message_count}: {reading.get('road_segment_id', 'UNKNOWN')}")
                    
                    # Process and predict
                    prediction = self.process_reading(reading)
                    
                    if prediction:
                        # Publish prediction
                        self.publish_prediction(prediction)
                        logger.info(f"Published prediction: {prediction['predictions']}")
                
                except json.JSONDecodeError as e:
                    logger.error(f"Failed to parse message: {e}")
                except Exception as e:
                    logger.error(f"Error processing message: {e}", exc_info=True)
        
        except KeyboardInterrupt:
            logger.info("Shutting down...")
        
        finally:
            # Cleanup
            self.consumer.close()
            self.producer.flush()
            self.data_loader.close_connections()
            logger.info("Service stopped")


def main():
    """Main entry point."""
    logger.info("=" * 80)
    logger.info("CityFlow Real-Time Prediction Service")
    logger.info("=" * 80)
    
    service = RealTimePredictionService()
    service.run()


if __name__ == "__main__":
    main()
