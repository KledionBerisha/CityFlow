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

# Save
print("\n4. Saving model...")
os.makedirs('models', exist_ok=True)
model_path = 'models/traffic_prediction_30min_simple.pkl'
joblib.dump(model, model_path)
print(f"[OK] Model saved: {model_path}")

# Test prediction
print("\n5. Test prediction...")
sample = X_test.iloc[0:1]
prediction = model.predict(sample)[0]
actual = y_test.iloc[0]
print(f"[OK] Sample prediction:")
print(f"  Input speed: {sample['speed_kmh'].values[0]:.1f} km/h")
print(f"  Predicted speed (30min): {prediction:.1f} km/h")
print(f"  Actual speed: {actual:.1f} km/h")
print(f"  Error: {abs(prediction - actual):.1f} km/h")

print("\n" + "="*80)
print("[SUCCESS] Training Complete!")
print("="*80)
print(f"\nModel file: {os.path.abspath(model_path)}")
print(f"Model size: {os.path.getsize(model_path) / 1024:.1f} KB")
