import { useEffect, useState } from 'react'
import * as api from '../services/api'

function Predict() {
  const [predictions, setPredictions] = useState<api.TrafficPrediction[]>([])

  useEffect(() => {
    const fetchPredictions = async () => {
      const data = await api.getTrafficPredictions()
      setPredictions(data)
    }
    fetchPredictions()
  }, [])

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold text-gray-800 mb-6">Predict</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Traffic Predictions</h2>
        {predictions.length === 0 ? (
          <p className="text-gray-600">No predictions available.</p>
        ) : (
          <ul className="space-y-2">
            {predictions.map((pred, idx) => (
              <li key={idx} className="border-b pb-2">
                <div className="font-medium">
                  Predicted Congestion: {pred.predictedCongestion}
                </div>
                <div className="text-sm text-gray-500">
                  Speed: {pred.predictedSpeed} km/h | Confidence: {pred.confidence}%
                </div>
              </li>
            ))}
          </ul>
        )}
        <p className="text-sm text-gray-500 mt-4">
          TODO: Connect to GET /api/analytics/predictions endpoint
        </p>
      </div>
    </div>
  )
}

export default Predict

