"""
CityFlow ML Pipeline - Model Training Module

Trains traffic prediction models using XGBoost and integrates with MLflow.
"""

import pandas as pd
import numpy as np
from typing import Dict, Tuple, List, Optional
import logging
from sklearn.model_selection import train_test_split, TimeSeriesSplit
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
from sklearn.preprocessing import LabelEncoder
import xgboost as xgb
import lightgbm as lgb
import joblib
import mlflow
import mlflow.xgboost
import mlflow.lightgbm
from datetime import datetime

logger = logging.getLogger(__name__)


class TrafficPredictionModel:
    """Train and evaluate traffic speed prediction models."""
    
    def __init__(self, config: Dict):
        self.config = config
        self.model = None
        self.feature_columns = []
        self.label_encoders = {}
        self.model_type = config['models']['traffic_speed_prediction']['type']
        
        # Set MLflow tracking URI
        mlflow.set_tracking_uri(config['mlflow']['tracking_uri'])
        mlflow.set_experiment(config['mlflow']['experiment_name'])
    
    def prepare_data(self, 
                    df: pd.DataFrame,
                    target_col: str,
                    test_size: float = 0.2) -> Tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
        """
        Prepare data for training.
        
        Args:
            df: DataFrame with engineered features
            target_col: Target column name
            test_size: Proportion of test set
            
        Returns:
            X_train, X_test, y_train, y_test
        """
        logger.info("Preparing data for training...")
        
        # Remove non-feature columns
        exclude_cols = [
            'timestamp', 'speed_kmh', 'vehicle_count', 'congestion_level',
            'latitude', 'longitude'
        ]
        
        # Also exclude target columns
        exclude_cols.extend([col for col in df.columns if 'future' in col])
        
        # Select feature columns
        self.feature_columns = [col for col in df.columns if col not in exclude_cols]
        
        # Handle categorical columns
        categorical_cols = df[self.feature_columns].select_dtypes(include=['object']).columns
        for col in categorical_cols:
            if col not in self.label_encoders:
                self.label_encoders[col] = LabelEncoder()
                df[col] = self.label_encoders[col].fit_transform(df[col].astype(str))
            else:
                df[col] = self.label_encoders[col].transform(df[col].astype(str))
        
        # Separate features and target
        X = df[self.feature_columns]
        y = df[target_col]
        
        # Remove rows with missing target
        mask = ~y.isna()
        X = X[mask]
        y = y[mask]
        
        # Split data (time-aware split)
        split_idx = int(len(X) * (1 - test_size))
        X_train = X.iloc[:split_idx]
        X_test = X.iloc[split_idx:]
        y_train = y.iloc[:split_idx]
        y_test = y.iloc[split_idx:]
        
        logger.info(f"Training set: {X_train.shape}, Test set: {X_test.shape}")
        logger.info(f"Features: {len(self.feature_columns)}")
        logger.info(f"Target range: [{y_train.min():.2f}, {y_train.max():.2f}]")
        
        return X_train, X_test, y_train, y_test
    
    def train_xgboost(self,
                     X_train: pd.DataFrame,
                     X_test: pd.DataFrame,
                     y_train: pd.Series,
                     y_test: pd.Series,
                     hyperparameters: Dict) -> xgb.XGBRegressor:
        """
        Train XGBoost model.
        
        Args:
            X_train, X_test: Feature matrices
            y_train, y_test: Target vectors
            hyperparameters: Model hyperparameters
            
        Returns:
            Trained model
        """
        logger.info("Training XGBoost model...")
        
        model = xgb.XGBRegressor(
            objective='reg:squarederror',
            random_state=self.config['training']['random_state'],
            **hyperparameters
        )
        
        # Train with early stopping
        model.fit(
            X_train, y_train,
            eval_set=[(X_test, y_test)],
            early_stopping_rounds=self.config['training']['early_stopping_rounds'],
            verbose=False
        )
        
        logger.info(f"Best iteration: {model.best_iteration}")
        
        return model
    
    def train_lightgbm(self,
                      X_train: pd.DataFrame,
                      X_test: pd.DataFrame,
                      y_train: pd.Series,
                      y_test: pd.Series,
                      hyperparameters: Dict) -> lgb.LGBMRegressor:
        """
        Train LightGBM model.
        
        Args:
            X_train, X_test: Feature matrices
            y_train, y_test: Target vectors
            hyperparameters: Model hyperparameters
            
        Returns:
            Trained model
        """
        logger.info("Training LightGBM model...")
        
        model = lgb.LGBMRegressor(
            objective='regression',
            random_state=self.config['training']['random_state'],
            verbose=-1,
            **hyperparameters
        )
        
        # Train with early stopping
        model.fit(
            X_train, y_train,
            eval_set=[(X_test, y_test)],
            callbacks=[
                lgb.early_stopping(self.config['training']['early_stopping_rounds']),
                lgb.log_evaluation(0)
            ]
        )
        
        return model
    
    def evaluate_model(self,
                      model,
                      X_test: pd.DataFrame,
                      y_test: pd.Series) -> Dict[str, float]:
        """
        Evaluate model performance.
        
        Args:
            model: Trained model
            X_test: Test features
            y_test: Test target
            
        Returns:
            Dictionary of metrics
        """
        logger.info("Evaluating model...")
        
        # Make predictions
        y_pred = model.predict(X_test)
        
        # Calculate metrics
        mse = mean_squared_error(y_test, y_pred)
        rmse = np.sqrt(mse)
        mae = mean_absolute_error(y_test, y_pred)
        r2 = r2_score(y_test, y_pred)
        
        # MAPE (Mean Absolute Percentage Error)
        mape = np.mean(np.abs((y_test - y_pred) / (y_test + 1e-6))) * 100
        
        metrics = {
            'mse': mse,
            'rmse': rmse,
            'mae': mae,
            'r2': r2,
            'mape': mape
        }
        
        logger.info("Model Performance:")
        logger.info(f"  RMSE: {rmse:.2f} km/h")
        logger.info(f"  MAE: {mae:.2f} km/h")
        logger.info(f"  R²: {r2:.4f}")
        logger.info(f"  MAPE: {mape:.2f}%")
        
        return metrics
    
    def get_feature_importance(self, model, top_n: int = 20) -> pd.DataFrame:
        """
        Get feature importance from model.
        
        Args:
            model: Trained model
            top_n: Number of top features to return
            
        Returns:
            DataFrame with feature importance
        """
        if hasattr(model, 'feature_importances_'):
            importance_df = pd.DataFrame({
                'feature': self.feature_columns,
                'importance': model.feature_importances_
            }).sort_values('importance', ascending=False)
            
            logger.info(f"\nTop {top_n} Most Important Features:")
            for idx, row in importance_df.head(top_n).iterrows():
                logger.info(f"  {row['feature']}: {row['importance']:.4f}")
            
            return importance_df.head(top_n)
        else:
            logger.warning("Model does not support feature importance")
            return pd.DataFrame()
    
    def train_and_log(self,
                     df: pd.DataFrame,
                     prediction_horizon: int = 30) -> Tuple[object, Dict]:
        """
        Complete training pipeline with MLflow logging.
        
        Args:
            df: DataFrame with engineered features
            prediction_horizon: Minutes ahead to predict
            
        Returns:
            Trained model and metrics
        """
        logger.info(f"Starting training pipeline for {prediction_horizon}-min prediction...")
        
        # Start MLflow run
        with mlflow.start_run(run_name=f"traffic_pred_{prediction_horizon}min_{datetime.now().strftime('%Y%m%d_%H%M%S')}"):
            
            # Log parameters
            mlflow.log_param("prediction_horizon_minutes", prediction_horizon)
            mlflow.log_param("model_type", self.model_type)
            mlflow.log_param("num_features", len(self.feature_columns))
            mlflow.log_param("train_days", self.config['training']['train_days'])
            
            # Prepare data
            target_col = f'speed_kmh_future_{prediction_horizon}min'
            X_train, X_test, y_train, y_test = self.prepare_data(
                df, 
                target_col,
                self.config['training']['test_size']
            )
            
            # Train model
            hyperparameters = self.config['models']['traffic_speed_prediction']['hyperparameters']
            mlflow.log_params(hyperparameters)
            
            if self.model_type == 'xgboost':
                self.model = self.train_xgboost(X_train, X_test, y_train, y_test, hyperparameters)
                mlflow.xgboost.log_model(self.model, "model")
            elif self.model_type == 'lightgbm':
                self.model = self.train_lightgbm(X_train, X_test, y_train, y_test, hyperparameters)
                mlflow.lightgbm.log_model(self.model, "model")
            else:
                raise ValueError(f"Unknown model type: {self.model_type}")
            
            # Evaluate
            metrics = self.evaluate_model(self.model, X_test, y_test)
            mlflow.log_metrics(metrics)
            
            # Feature importance
            importance_df = self.get_feature_importance(self.model)
            if not importance_df.empty:
                importance_df.to_csv("feature_importance.csv", index=False)
                mlflow.log_artifact("feature_importance.csv")
            
            # Log feature columns
            with open("feature_columns.txt", "w") as f:
                f.write("\n".join(self.feature_columns))
            mlflow.log_artifact("feature_columns.txt")
            
            # Register model
            model_name = f"cityflow_traffic_prediction_{prediction_horizon}min"
            mlflow.register_model(
                f"runs:/{mlflow.active_run().info.run_id}/model",
                model_name
            )
            
            logger.info(f"Model registered as: {model_name}")
            logger.info(f"MLflow Run ID: {mlflow.active_run().info.run_id}")
        
        return self.model, metrics
    
    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """
        Make predictions with trained model.
        
        Args:
            X: Feature matrix
            
        Returns:
            Array of predictions
        """
        if self.model is None:
            raise ValueError("Model not trained. Call train_and_log() first.")
        
        # Ensure columns match training
        X = X[self.feature_columns]
        
        # Handle categorical encoding
        for col in self.label_encoders:
            if col in X.columns:
                X[col] = self.label_encoders[col].transform(X[col].astype(str))
        
        return self.model.predict(X)
    
    def save_model(self, filepath: str):
        """Save model to disk."""
        if self.model is None:
            raise ValueError("No model to save")
        
        joblib.dump({
            'model': self.model,
            'feature_columns': self.feature_columns,
            'label_encoders': self.label_encoders,
            'config': self.config
        }, filepath)
        
        logger.info(f"Model saved to {filepath}")
    
    def load_model(self, filepath: str):
        """Load model from disk."""
        data = joblib.load(filepath)
        self.model = data['model']
        self.feature_columns = data['feature_columns']
        self.label_encoders = data['label_encoders']
        
        logger.info(f"Model loaded from {filepath}")


class CongestionClassificationModel:
    """Train congestion classification models."""
    
    def __init__(self, config: Dict):
        self.config = config
        self.model = None
        self.feature_columns = []
        self.label_encoder = LabelEncoder()
        
        mlflow.set_tracking_uri(config['mlflow']['tracking_uri'])
        mlflow.set_experiment(config['mlflow']['experiment_name'])
    
    def prepare_data(self, df: pd.DataFrame) -> Tuple:
        """Prepare data for classification."""
        logger.info("Preparing data for congestion classification...")
        
        # Encode target
        y = self.label_encoder.fit_transform(df['congestion_level'])
        
        # Select features (similar to regression)
        exclude_cols = ['timestamp', 'congestion_level', 'speed_kmh', 'latitude', 'longitude']
        self.feature_columns = [col for col in df.columns if col not in exclude_cols]
        
        X = df[self.feature_columns]
        
        # Split
        test_size = self.config['training']['test_size']
        split_idx = int(len(X) * (1 - test_size))
        
        X_train = X.iloc[:split_idx]
        X_test = X.iloc[split_idx:]
        y_train = y[:split_idx]
        y_test = y[split_idx:]
        
        logger.info(f"Classes: {list(self.label_encoder.classes_)}")
        logger.info(f"Training set: {X_train.shape}, Test set: {X_test.shape}")
        
        return X_train, X_test, y_train, y_test
    
    def train_and_log(self, df: pd.DataFrame):
        """Train classification model with MLflow logging."""
        logger.info("Training congestion classification model...")
        
        with mlflow.start_run(run_name=f"congestion_classification_{datetime.now().strftime('%Y%m%d_%H%M%S')}"):
            
            # Prepare data
            X_train, X_test, y_train, y_test = self.prepare_data(df)
            
            # Train LightGBM classifier
            hyperparameters = self.config['models']['congestion_classification']['hyperparameters']
            mlflow.log_params(hyperparameters)
            
            self.model = lgb.LGBMClassifier(
                objective='multiclass',
                num_class=len(self.label_encoder.classes_),
                random_state=self.config['training']['random_state'],
                verbose=-1,
                **hyperparameters
            )
            
            self.model.fit(X_train, y_train)
            
            # Evaluate
            from sklearn.metrics import accuracy_score, precision_recall_fscore_support
            
            y_pred = self.model.predict(X_test)
            accuracy = accuracy_score(y_test, y_pred)
            precision, recall, f1, _ = precision_recall_fscore_support(y_test, y_pred, average='weighted')
            
            metrics = {
                'accuracy': accuracy,
                'precision': precision,
                'recall': recall,
                'f1': f1
            }
            
            mlflow.log_metrics(metrics)
            mlflow.lightgbm.log_model(self.model, "model")
            
            logger.info(f"Accuracy: {accuracy:.4f}, F1: {f1:.4f}")
            
            # Register model
            mlflow.register_model(
                f"runs:/{mlflow.active_run().info.run_id}/model",
                "cityflow_congestion_classification"
            )
        
        return self.model, metrics
