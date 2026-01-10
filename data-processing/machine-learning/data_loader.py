"""
CityFlow ML Pipeline - Data Loader Module

Loads and prepares data from various sources (Delta Lake, PostgreSQL, MongoDB).
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Optional, Dict, List
import logging
from sqlalchemy import create_engine
from pymongo import MongoClient
import redis

logger = logging.getLogger(__name__)


class DataLoader:
    """Load traffic data from multiple sources."""
    
    def __init__(self, config: Dict):
        self.config = config
        self.postgres_engine = None
        self.mongo_client = None
        self.redis_client = None
        
    def _get_postgres_engine(self):
        """Initialize PostgreSQL connection."""
        if self.postgres_engine is None:
            pg_config = self.config['data']['postgres']
            connection_string = (
                f"postgresql://{pg_config['user']}:{pg_config['password']}"
                f"@{pg_config['host']}:{pg_config['port']}/{pg_config['database']}"
            )
            self.postgres_engine = create_engine(connection_string)
            logger.info("PostgreSQL connection established")
        return self.postgres_engine
    
    def _get_mongo_client(self):
        """Initialize MongoDB connection."""
        if self.mongo_client is None:
            mongo_config = self.config['data']['mongodb']
            self.mongo_client = MongoClient(mongo_config['uri'])
            logger.info("MongoDB connection established")
        return self.mongo_client
    
    def _get_redis_client(self):
        """Initialize Redis connection."""
        if self.redis_client is None:
            redis_config = self.config['data']['redis']
            self.redis_client = redis.Redis(
                host=redis_config['host'],
                port=redis_config['port'],
                db=redis_config['db'],
                decode_responses=True
            )
            logger.info("Redis connection established")
        return self.redis_client
    
    def load_from_postgres(self, 
                          table_name: str,
                          start_date: Optional[datetime] = None,
                          end_date: Optional[datetime] = None,
                          columns: Optional[List[str]] = None) -> pd.DataFrame:
        """
        Load data from PostgreSQL.
        
        Args:
            table_name: Name of the table to load
            start_date: Start date filter
            end_date: End date filter
            columns: Specific columns to load
            
        Returns:
            DataFrame with loaded data
        """
        logger.info(f"Loading data from PostgreSQL table: {table_name}")
        
        engine = self._get_postgres_engine()
        
        # Build query
        query = f"SELECT {'*' if columns is None else ', '.join(columns)} FROM {table_name}"
        
        if start_date or end_date:
            query += " WHERE "
            conditions = []
            if start_date:
                conditions.append(f"timestamp >= '{start_date}'")
            if end_date:
                conditions.append(f"timestamp <= '{end_date}'")
            query += " AND ".join(conditions)
        
        query += " ORDER BY timestamp"
        
        logger.info(f"Executing query: {query}")
        df = pd.read_sql(query, engine)
        
        logger.info(f"Loaded {len(df)} rows from PostgreSQL")
        return df
    
    def load_aggregated_traffic(self,
                               window: str = '30min',
                               days: int = 30) -> pd.DataFrame:
        """
        Load aggregated traffic data from PostgreSQL.
        
        Args:
            window: Aggregation window ('5min', '15min', '30min')
            days: Number of days to load
            
        Returns:
            DataFrame with aggregated traffic data
        """
        table_name = f"traffic_aggregated_{window}"
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)
        
        logger.info(f"Loading {days} days of aggregated traffic data ({window} window)")
        
        df = self.load_from_postgres(
            table_name=table_name,
            start_date=start_date,
            end_date=end_date
        )
        
        # Rename columns to standard format
        column_mapping = {
            'window_start': 'timestamp',
            'avg_speed': 'speed_kmh',
            'total_vehicles': 'vehicle_count'
        }
        df = df.rename(columns=column_mapping)
        
        return df
    
    def load_from_delta_lake(self,
                            table_path: str,
                            start_date: Optional[datetime] = None,
                            end_date: Optional[datetime] = None) -> pd.DataFrame:
        """
        Load data from Delta Lake (requires Spark or delta-rs).
        
        Note: For simplicity, we'll use pyarrow to read parquet files.
        In production, use PySpark or delta-rs for full Delta Lake features.
        
        Args:
            table_path: Path to Delta Lake table
            start_date: Start date filter
            end_date: End date filter
            
        Returns:
            DataFrame with loaded data
        """
        logger.info(f"Loading data from Delta Lake: {table_path}")
        
        try:
            import pyarrow.parquet as pq
            
            # Read parquet files
            df = pd.read_parquet(table_path)
            
            # Apply date filters
            if 'timestamp' in df.columns:
                df['timestamp'] = pd.to_datetime(df['timestamp'])
                if start_date:
                    df = df[df['timestamp'] >= start_date]
                if end_date:
                    df = df[df['timestamp'] <= end_date]
            
            logger.info(f"Loaded {len(df)} rows from Delta Lake")
            return df
            
        except Exception as e:
            logger.error(f"Error loading from Delta Lake: {e}")
            logger.info("Falling back to PostgreSQL...")
            return self.load_aggregated_traffic(days=30)
    
    def load_training_data(self, days: int = 30) -> pd.DataFrame:
        """
        Load complete training dataset with all required features.
        
        Args:
            days: Number of days of historical data to load
            
        Returns:
            DataFrame ready for feature engineering
        """
        logger.info(f"Loading training data for last {days} days...")
        
        # Try Delta Lake first, fallback to PostgreSQL
        try:
            delta_path = f"{self.config['data']['delta_lake_path']}/traffic/aggregated_5min"
            df = self.load_from_delta_lake(delta_path, 
                                          start_date=datetime.now() - timedelta(days=days))
        except Exception as e:
            logger.warning(f"Could not load from Delta Lake: {e}")
            df = self.load_aggregated_traffic(window='5min', days=days)
        
        # Ensure required columns exist
        required_cols = ['timestamp', 'road_segment_id', 'speed_kmh']
        missing_cols = [col for col in required_cols if col not in df.columns]
        
        if missing_cols:
            raise ValueError(f"Missing required columns: {missing_cols}")
        
        # Add mock road_segment_id if not present
        if 'road_segment_id' not in df.columns:
            logger.warning("road_segment_id not found, creating synthetic segments")
            df['road_segment_id'] = 'SEGMENT_001'
        
        # Sort by time
        df = df.sort_values(['road_segment_id', 'timestamp'])
        
        logger.info(f"Training data loaded: {df.shape}")
        logger.info(f"Date range: {df['timestamp'].min()} to {df['timestamp'].max()}")
        logger.info(f"Road segments: {df['road_segment_id'].nunique()}")
        
        return df
    
    def load_prediction_data(self, minutes: int = 60) -> pd.DataFrame:
        """
        Load recent data for making predictions.
        
        Args:
            minutes: Number of recent minutes to load
            
        Returns:
            DataFrame with recent data
        """
        logger.info(f"Loading prediction data for last {minutes} minutes...")
        
        end_date = datetime.now()
        start_date = end_date - timedelta(minutes=minutes)
        
        # Load from PostgreSQL (most recent aggregated data)
        df = self.load_aggregated_traffic(window='5min', days=1)
        
        # Filter to recent time window
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df[df['timestamp'] >= start_date]
        
        logger.info(f"Loaded {len(df)} recent records for prediction")
        return df
    
    def generate_synthetic_data(self, 
                               num_days: int = 30,
                               num_segments: int = 10,
                               freq: str = '5min') -> pd.DataFrame:
        """
        Generate synthetic traffic data for testing when real data is unavailable.
        
        Args:
            num_days: Number of days to generate
            num_segments: Number of road segments
            freq: Data frequency (e.g., '5min')
            
        Returns:
            DataFrame with synthetic traffic data
        """
        logger.info(f"Generating synthetic data: {num_days} days, {num_segments} segments")
        
        # Create date range
        end_date = datetime.now()
        start_date = end_date - timedelta(days=num_days)
        timestamps = pd.date_range(start=start_date, end=end_date, freq=freq)
        
        # Create data for each segment
        data = []
        for segment_id in range(1, num_segments + 1):
            segment_name = f"SEGMENT_{segment_id:03d}"
            
            for ts in timestamps:
                # Generate realistic traffic patterns
                hour = ts.hour
                day_of_week = ts.dayofweek
                
                # Base speed varies by time of day
                if 7 <= hour <= 9 or 17 <= hour <= 19:  # Rush hour
                    base_speed = 25 + np.random.normal(0, 5)
                    vehicle_count = 80 + np.random.poisson(20)
                elif 0 <= hour <= 5:  # Night
                    base_speed = 55 + np.random.normal(0, 5)
                    vehicle_count = 10 + np.random.poisson(5)
                else:  # Normal
                    base_speed = 40 + np.random.normal(0, 5)
                    vehicle_count = 40 + np.random.poisson(10)
                
                # Weekend adjustment
                if day_of_week >= 5:
                    base_speed += 5
                    vehicle_count *= 0.7
                
                # Add some noise
                speed = max(10, base_speed + np.random.normal(0, 3))
                vehicles = max(0, int(vehicle_count + np.random.normal(0, 5)))
                
                # Congestion level based on speed
                if speed > 50:
                    congestion = "FREE_FLOW"
                elif speed > 35:
                    congestion = "MODERATE"
                elif speed > 20:
                    congestion = "HEAVY"
                else:
                    congestion = "SEVERE"
                
                data.append({
                    'timestamp': ts,
                    'road_segment_id': segment_name,
                    'speed_kmh': speed,
                    'vehicle_count': vehicles,
                    'congestion_level': congestion,
                    'latitude': 42.0 + (segment_id * 0.01),
                    'longitude': 21.0 + (segment_id * 0.01)
                })
        
        df = pd.DataFrame(data)
        logger.info(f"Generated {len(df)} synthetic records")
        
        return df
    
    def close_connections(self):
        """Close all database connections."""
        if self.postgres_engine:
            self.postgres_engine.dispose()
            logger.info("PostgreSQL connection closed")
        
        if self.mongo_client:
            self.mongo_client.close()
            logger.info("MongoDB connection closed")
        
        if self.redis_client:
            self.redis_client.close()
            logger.info("Redis connection closed")
