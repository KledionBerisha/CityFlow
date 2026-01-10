"""
CityFlow ML Pipeline - Feature Engineering Module

Extracts and transforms features from raw traffic data for model training and prediction.
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Dict, List, Tuple
import logging

logger = logging.getLogger(__name__)


class TrafficFeatureEngineer:
    """Feature engineering for traffic prediction models."""
    
    def __init__(self, config: Dict):
        self.config = config
        self.feature_names = []
        
    def create_temporal_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Create time-based features from timestamp.
        
        Args:
            df: DataFrame with 'timestamp' column
            
        Returns:
            DataFrame with added temporal features
        """
        logger.info("Creating temporal features...")
        
        df = df.copy()
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        
        # Basic temporal features
        df['hour_of_day'] = df['timestamp'].dt.hour
        df['day_of_week'] = df['timestamp'].dt.dayofweek
        df['day_of_month'] = df['timestamp'].dt.day
        df['month'] = df['timestamp'].dt.month
        df['year'] = df['timestamp'].dt.year
        
        # Cyclical encoding (important for time!)
        df['hour_sin'] = np.sin(2 * np.pi * df['hour_of_day'] / 24)
        df['hour_cos'] = np.cos(2 * np.pi * df['hour_of_day'] / 24)
        df['day_sin'] = np.sin(2 * np.pi * df['day_of_week'] / 7)
        df['day_cos'] = np.cos(2 * np.pi * df['day_of_week'] / 7)
        df['month_sin'] = np.sin(2 * np.pi * df['month'] / 12)
        df['month_cos'] = np.cos(2 * np.pi * df['month'] / 12)
        
        # Boolean features
        df['is_weekend'] = (df['day_of_week'] >= 5).astype(int)
        df['is_rush_hour_morning'] = df['hour_of_day'].between(7, 9).astype(int)
        df['is_rush_hour_evening'] = df['hour_of_day'].between(17, 19).astype(int)
        df['is_rush_hour'] = (df['is_rush_hour_morning'] | df['is_rush_hour_evening']).astype(int)
        df['is_night'] = df['hour_of_day'].between(0, 5).astype(int)
        
        # Season
        df['season'] = df['month'].apply(self._get_season)
        
        logger.info(f"Created {len(df.columns)} temporal features")
        return df
    
    def create_lag_features(self, df: pd.DataFrame, 
                           target_cols: List[str],
                           lags: List[int]) -> pd.DataFrame:
        """
        Create lag features for time series prediction.
        
        Args:
            df: DataFrame sorted by timestamp
            target_cols: Columns to create lags for (e.g., ['speed_kmh', 'vehicle_count'])
            lags: List of lag periods in minutes (e.g., [5, 15, 30, 60])
            
        Returns:
            DataFrame with lag features
        """
        logger.info(f"Creating lag features for {target_cols} with lags {lags}...")
        
        df = df.copy()
        df = df.sort_values(['road_segment_id', 'timestamp'])
        
        for col in target_cols:
            for lag_minutes in lags:
                lag_col_name = f'{col}_lag_{lag_minutes}min'
                
                # Calculate lag based on 5-minute intervals (assuming data comes every 5 min)
                lag_periods = lag_minutes // 5
                df[lag_col_name] = df.groupby('road_segment_id')[col].shift(lag_periods)
                
        logger.info(f"Created {len(target_cols) * len(lags)} lag features")
        return df
    
    def create_rolling_features(self, df: pd.DataFrame,
                               target_cols: List[str],
                               windows: List[int]) -> pd.DataFrame:
        """
        Create rolling window statistics.
        
        Args:
            df: DataFrame sorted by timestamp
            target_cols: Columns to compute rolling stats
            windows: Window sizes in minutes
            
        Returns:
            DataFrame with rolling features
        """
        logger.info(f"Creating rolling window features for {target_cols}...")
        
        df = df.copy()
        df = df.sort_values(['road_segment_id', 'timestamp'])
        
        for col in target_cols:
            for window_minutes in windows:
                window_periods = window_minutes // 5  # Assuming 5-min intervals
                
                # Rolling mean
                df[f'{col}_rolling_mean_{window_minutes}min'] = (
                    df.groupby('road_segment_id')[col]
                    .rolling(window=window_periods, min_periods=1)
                    .mean()
                    .reset_index(0, drop=True)
                )
                
                # Rolling std
                df[f'{col}_rolling_std_{window_minutes}min'] = (
                    df.groupby('road_segment_id')[col]
                    .rolling(window=window_periods, min_periods=1)
                    .std()
                    .reset_index(0, drop=True)
                )
                
                # Rolling min/max
                df[f'{col}_rolling_min_{window_minutes}min'] = (
                    df.groupby('road_segment_id')[col]
                    .rolling(window=window_periods, min_periods=1)
                    .min()
                    .reset_index(0, drop=True)
                )
                
                df[f'{col}_rolling_max_{window_minutes}min'] = (
                    df.groupby('road_segment_id')[col]
                    .rolling(window=window_periods, min_periods=1)
                    .max()
                    .reset_index(0, drop=True)
                )
                
        logger.info(f"Created rolling window features")
        return df
    
    def create_historical_features(self, df: pd.DataFrame,
                                   historical_df: pd.DataFrame) -> pd.DataFrame:
        """
        Create features based on historical patterns.
        
        Args:
            df: Current DataFrame
            historical_df: Historical data for computing patterns
            
        Returns:
            DataFrame with historical features
        """
        logger.info("Creating historical pattern features...")
        
        df = df.copy()
        
        # Average speed by hour of day and road segment (historical pattern)
        historical_hourly_avg = historical_df.groupby(
            ['road_segment_id', 'hour_of_day']
        )['speed_kmh'].mean().reset_index()
        historical_hourly_avg.columns = ['road_segment_id', 'hour_of_day', 'historical_avg_speed']
        
        df = df.merge(historical_hourly_avg, on=['road_segment_id', 'hour_of_day'], how='left')
        
        # Average speed by day of week and road segment
        historical_daily_avg = historical_df.groupby(
            ['road_segment_id', 'day_of_week']
        )['speed_kmh'].mean().reset_index()
        historical_daily_avg.columns = ['road_segment_id', 'day_of_week', 'historical_daily_avg_speed']
        
        df = df.merge(historical_daily_avg, on=['road_segment_id', 'day_of_week'], how='left')
        
        # Deviation from historical average
        df['speed_deviation_from_historical'] = df['speed_kmh'] - df['historical_avg_speed']
        
        logger.info("Created historical pattern features")
        return df
    
    def create_geospatial_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Create geospatial features from coordinates.
        
        Args:
            df: DataFrame with latitude/longitude
            
        Returns:
            DataFrame with geospatial features
        """
        logger.info("Creating geospatial features...")
        
        df = df.copy()
        
        # Encode road segment ID (one-hot or target encoding depending on cardinality)
        segment_counts = df['road_segment_id'].value_counts()
        
        if len(segment_counts) < 50:
            # One-hot encode if few segments (preserve original column for groupby operations)
            segment_dummies = pd.get_dummies(df['road_segment_id'], prefix='segment')
            df = pd.concat([df, segment_dummies], axis=1)
        else:
            # Target encode if many segments
            segment_target_mean = df.groupby('road_segment_id')['speed_kmh'].mean()
            df['segment_avg_speed'] = df['road_segment_id'].map(segment_target_mean)
        
        logger.info("Created geospatial features")
        return df
    
    def create_congestion_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Create congestion-related features.
        
        Args:
            df: DataFrame with traffic data
            
        Returns:
            DataFrame with congestion features
        """
        logger.info("Creating congestion features...")
        
        df = df.copy()
        
        # Congestion intensity (vehicles per km)
        if 'vehicle_count' in df.columns:
            df['vehicle_density'] = df['vehicle_count'] / 1.0  # Assume 1km segments
        
        # Speed ratio (current speed / historical average)
        if 'historical_avg_speed' in df.columns:
            df['speed_ratio'] = df['speed_kmh'] / (df['historical_avg_speed'] + 1e-6)
        
        # Congestion trend (speed change over time)
        df['speed_change_5min'] = df.groupby('road_segment_id')['speed_kmh'].diff(1)
        
        logger.info("Created congestion features")
        return df
    
    def prepare_features(self, df: pd.DataFrame,
                        historical_df: pd.DataFrame = None,
                        is_training: bool = True) -> pd.DataFrame:
        """
        Complete feature engineering pipeline.
        
        Args:
            df: Raw traffic data
            historical_df: Historical data for pattern extraction
            is_training: Whether this is for training (affects lag feature handling)
            
        Returns:
            DataFrame with all engineered features
        """
        logger.info("Starting complete feature engineering pipeline...")
        
        # Step 1: Temporal features
        df = self.create_temporal_features(df)
        
        # Step 2: Lag features
        lag_cols = ['speed_kmh', 'vehicle_count'] if 'vehicle_count' in df.columns else ['speed_kmh']
        lags = [5, 15, 30, 60]
        df = self.create_lag_features(df, lag_cols, lags)
        
        # Step 3: Rolling features
        windows = [15, 30, 60]
        df = self.create_rolling_features(df, lag_cols, windows)
        
        # Step 4: Historical features (if historical data provided)
        if historical_df is not None:
            historical_df = self.create_temporal_features(historical_df)
            df = self.create_historical_features(df, historical_df)
        
        # Step 5: Geospatial features
        df = self.create_geospatial_features(df)
        
        # Step 6: Congestion features
        df = self.create_congestion_features(df)
        
        # Drop rows with NaN from lag/rolling features if training
        if is_training:
            df = df.dropna()
        
        logger.info(f"Feature engineering complete. Final shape: {df.shape}")
        logger.info(f"Feature columns: {list(df.columns)}")
        
        return df
    
    @staticmethod
    def _get_season(month: int) -> int:
        """Map month to season (0-3)."""
        if month in [12, 1, 2]:
            return 0  # Winter
        elif month in [3, 4, 5]:
            return 1  # Spring
        elif month in [6, 7, 8]:
            return 2  # Summer
        else:
            return 3  # Fall


def create_target_variable(df: pd.DataFrame, 
                          target_col: str,
                          prediction_horizon_minutes: int) -> pd.DataFrame:
    """
    Create target variable for prediction (future value).
    
    Args:
        df: DataFrame sorted by timestamp
        target_col: Column to predict (e.g., 'speed_kmh')
        prediction_horizon_minutes: How many minutes ahead to predict
        
    Returns:
        DataFrame with target column added
    """
    df = df.copy()
    df = df.sort_values(['road_segment_id', 'timestamp'])
    
    # Calculate future value
    future_periods = prediction_horizon_minutes // 5
    target_name = f'{target_col}_future_{prediction_horizon_minutes}min'
    
    df[target_name] = df.groupby('road_segment_id')[target_col].shift(-future_periods)
    
    return df
