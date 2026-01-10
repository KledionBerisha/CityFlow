"""
Simple ML Model Training (No MLflow Required)
"""
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, r2_score
import xgboost as xgb
import joblib
import os
from datetime import datetime, timedelta

print("="*80)
print("CityFlow Simple ML Training (No MLflow)")
print("="*80)

# Generate simple synthetic data
print("\n1. Generating synthetic data...")
np.random.seed(42)
n_samples = 5000
dates = pd.date_range(end=datetime.now(), periods=n_samples, freq='5min')

data = {
    'timestamp': dates,
    'road_segment_id': np.random.choice(['SEG_001', 'SEG_002', 'SEG_003'], n_samples),
    'speed_kmh': np.random.uniform(20, 80, n_samples),
    'vehicle_count': np.random.randint(10, 200, n_samples),
    'hour_of_day': [d.hour for d in dates],
    'day_of_week': [d.dayofweek for d in dates],
    'is_weekend': [1 if d.dayofweek >= 5 else 0 for d in dates]
}

df = pd.DataFrame(data)

# Create target: speed 30 minutes ahead
df['target_speed'] = df.groupby('road_segment_id')['speed_kmh'].shift(-6)  # 6*5min = 30min
df = df.dropna()

print(f"[OK] Data generated: {len(df)} samples")

# Features
feature_cols = ['speed_kmh', 'vehicle_count', 'hour_of_day', 'day_of_week', 'is_weekend']
X = df[feature_cols]
y = df['target_speed']

print(f"[OK] Features: {feature_cols}")
print(f"[OK] Target: target_speed (30-min ahead)")

# Split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
print(f"[OK] Train: {len(X_train)}, Test: {len(X_test)}")

# Train
print("\n2. Training XGBoost model...")
model = xgb.XGBRegressor(
    n_estimators=100,
    max_depth=6,
    learning_rate=0.1,
    random_state=42,
    n_jobs=-1
)

model.fit(X_train, y_train)
print("[OK] Model trained")

# Evaluate
print("\n3. Evaluating model...")
y_pred = model.predict(X_test)
mse = mean_squared_error(y_test, y_pred)
rmse = np.sqrt(mse)
r2 = r2_score(y_test, y_pred)

print(f"[OK] RMSE: {rmse:.2f} km/h")
print(f"[OK] R2: {r2:.4f}")

# Save model in expected format (with feature columns and encoders)
print("\n4. Saving model...")
os.makedirs('models', exist_ok=True)

# Save in format expected by model_serving_api.py
model_data = {
    'model': model,
    'feature_columns': feature_cols,  # Store feature column names
    'label_encoders': {},  # No categorical encoding needed for this simple model
    'config': {
        'prediction_horizon': 30,
        'model_type': 'xgboost',
        'version': '1.0.0'
    }
}

# Save as traffic_pred_30min.pkl (expected format)
model_path = 'models/traffic_pred_30min.pkl'
joblib.dump(model_data, model_path)
print(f"[OK] Model saved: {model_path}")

# Also train and save 10min and 20min models
print("\n5. Training additional models for 10min and 20min horizons...")
for horizon in [10, 20]:
    if horizon == 30:
        continue  # Already trained
    
    # Create target for this horizon
    shifts = horizon // 5  # 5-minute intervals
    df_horizon = df.copy()
    df_horizon['target_speed'] = df_horizon.groupby('road_segment_id')['speed_kmh'].shift(-shifts)
    df_horizon = df_horizon.dropna()
    
    if len(df_horizon) < 100:
        print(f"[SKIP] Not enough data for {horizon}min model")
        continue
    
    X_h = df_horizon[feature_cols]
    y_h = df_horizon['target_speed']
    
    X_train_h, X_test_h, y_train_h, y_test_h = train_test_split(X_h, y_h, test_size=0.2, random_state=42)
    
    model_h = xgb.XGBRegressor(
        n_estimators=100,
        max_depth=6,
        learning_rate=0.1,
        random_state=42,
        n_jobs=-1
    )
    
    model_h.fit(X_train_h, y_train_h)
    
    # Evaluate
    y_pred_h = model_h.predict(X_test_h)
    rmse_h = np.sqrt(mean_squared_error(y_test_h, y_pred_h))
    r2_h = r2_score(y_test_h, y_pred_h)
    
    print(f"[OK] {horizon}min model - RMSE: {rmse_h:.2f}, R2: {r2_h:.4f}")
    
    # Save
    model_data_h = {
        'model': model_h,
        'feature_columns': feature_cols,
        'label_encoders': {},
        'config': {
            'prediction_horizon': horizon,
            'model_type': 'xgboost',
            'version': '1.0.0'
        }
    }
    
    model_path_h = f'models/traffic_pred_{horizon}min.pkl'
    joblib.dump(model_data_h, model_path_h)
    print(f"[OK] Model saved: {model_path_h}")

# Test prediction
print("\n6. Test prediction...")
sample = X_test.iloc[0:1]
prediction = model.predict(sample)[0]
actual = y_test.iloc[0]
print(f"[OK] Sample prediction (30min):")
print(f"  Input speed: {sample['speed_kmh'].values[0]:.1f} km/h")
print(f"  Predicted speed: {prediction:.1f} km/h")
print(f"  Actual speed: {actual:.1f} km/h")
print(f"  Error: {abs(prediction - actual):.1f} km/h")

print("\n" + "="*80)
print("[SUCCESS] Training Complete!")
print("="*80)
print(f"\nModels saved:")
for horizon in [10, 20, 30]:
    model_file = f'models/traffic_pred_{horizon}min.pkl'
    if os.path.exists(model_file):
        size_kb = os.path.getsize(model_file) / 1024
        print(f"  - {model_file} ({size_kb:.1f} KB)")
print(f"\nNext steps:")
print("1. Restart ML API: docker-compose restart ml-api")
print("2. Check health: http://localhost:8090/health")
print("3. Test predictions: http://localhost:8090/predict/all?horizon=30")
