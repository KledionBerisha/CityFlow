import { useState, useEffect } from 'react'
import { useMLPredictions } from '../hooks/useMLPredictions'
import * as api from '../services/api'
import { TrendingUp, TrendingDown, Minus, AlertCircle, CheckCircle, Loader, Clock, MapPin } from 'lucide-react'

function Predict() {
  const { predictions, loading, error, mlApiAvailable, refetch } = useMLPredictions(30000) // Refresh every 30s
  const [hotspots, setHotspots] = useState<any[]>([])
  const [loadingHotspots, setLoadingHotspots] = useState(false)

  const getPredictionTrend = (change: number) => {
    if (change > 5) return { icon: TrendingDown, color: 'text-green-600', label: 'Improving' }
    if (change < -5) return { icon: TrendingUp, color: 'text-red-600', label: 'Worsening' }
    return { icon: Minus, color: 'text-gray-600', label: 'Stable' }
  }

  const getCongestionLevel = (speed: number) => {
    if (speed > 50) return { label: 'Free Flow', color: 'bg-green-100 text-green-800' }
    if (speed > 35) return { label: 'Moderate', color: 'bg-yellow-100 text-yellow-800' }
    if (speed > 20) return { label: 'Heavy', color: 'bg-orange-100 text-orange-800' }
    return { label: 'Severe', color: 'bg-red-100 text-red-800' }
  }

  const getCongestionLevelBySeverity = (severity: number) => {
    if (severity < 0.2) return { label: 'Free Flow', color: 'bg-green-100 text-green-800' }
    if (severity < 0.4) return { label: 'Moderate', color: 'bg-yellow-100 text-yellow-800' }
    if (severity < 0.7) return { label: 'Heavy', color: 'bg-orange-100 text-orange-800' }
    return { label: 'Severe', color: 'bg-red-100 text-red-800' }
  }

  // Fetch congestion hotspots
  useEffect(() => {
    const fetchHotspots = async () => {
      if (!mlApiAvailable) return
      
      setLoadingHotspots(true)
      try {
        const data = await api.getCongestionHotspots()
        setHotspots(Array.isArray(data) ? data : [])
      } catch (err) {
        console.error('Error fetching hotspots:', err)
        setHotspots([])
      } finally {
        setLoadingHotspots(false)
      }
    }

    fetchHotspots()
    const interval = setInterval(fetchHotspots, 30000) // Refresh every 30s
    return () => clearInterval(interval)
  }, [mlApiAvailable])

  return (
    <div className="p-8">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-800">Traffic Predictions & Congestion Analysis</h1>
          <p className="text-gray-600 mt-2">ML-powered traffic speed forecasts and congestion duration predictions</p>
        </div>

      {/* Status Banner */}
      <div className={`mb-6 p-4 rounded-lg flex items-center gap-3 ${
        mlApiAvailable ? 'bg-green-50 border border-green-200' : 'bg-yellow-50 border border-yellow-200'
      }`}>
        {mlApiAvailable ? (
          <>
            <CheckCircle className="text-green-600" size={20} />
            <span className="text-green-800 font-medium">ML Prediction Service Active</span>
          </>
        ) : (
          <>
            <AlertCircle className="text-yellow-600" size={20} />
            <span className="text-yellow-800 font-medium">ML Prediction Service Unavailable</span>
            <span className="text-yellow-600 text-sm ml-2">(Start ML API: docker-compose up -d)</span>
          </>
        )}
        
        <button
          onClick={refetch}
          className="ml-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          Refresh
        </button>
      </div>

      {/* Loading State */}
      {loading && (
        <div className="flex items-center justify-center py-12">
          <Loader className="animate-spin text-blue-600" size={32} />
          <span className="ml-3 text-gray-600">Loading predictions...</span>
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <AlertCircle className="mx-auto text-red-600 mb-3" size={32} />
          <p className="text-red-800 font-medium">Error loading predictions</p>
          <p className="text-red-600 text-sm mt-2">{error}</p>
        </div>
      )}

      {/* Congestion Hotspots Section */}
      {!loadingHotspots && hotspots.length > 0 && (
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-gray-800 mb-4">🚨 Biggest Congestions</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
            {hotspots.slice(0, 6).map((hotspot, idx) => {
              const congestion = getCongestionLevelBySeverity(hotspot.congestion_severity)
              const duration = hotspot.duration_prediction
              
              return (
                <div key={hotspot.road_segment_id || idx} className="bg-white rounded-lg shadow hover:shadow-lg transition p-5 border-l-4 border-red-500">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <MapPin size={16} className="text-gray-500" />
                        <h3 className="font-semibold text-gray-800 text-sm">{hotspot.road_segment_id}</h3>
                        <span className={`px-2 py-0.5 rounded text-xs font-medium ${congestion.color}`}>
                          {congestion.label}
                        </span>
                      </div>
                      <div className="text-xs text-gray-600 mb-2">
                        <div>Current: {hotspot.current_speed_kmh?.toFixed(1) || 'N/A'} km/h</div>
                        <div>Predicted ({hotspot.prediction_horizon_minutes}min): {hotspot.predicted_speed_kmh?.toFixed(1) || 'N/A'} km/h</div>
                      </div>
                      {duration && (
                        <div className="mt-3 pt-3 border-t border-gray-200">
                          <div className="flex items-center gap-2 text-xs">
                            <Clock size={14} className="text-orange-600" />
                            <span className="text-gray-700 font-medium">
                              Duration: ~{duration.predicted_duration_minutes} min
                            </span>
                          </div>
                          <div className="text-xs text-gray-500 mt-1">
                            Expected clear: {new Date(duration.expected_clear_time).toLocaleTimeString()}
                          </div>
                          <div className="text-xs text-gray-400 mt-1">
                            Confidence: {duration.confidence || 'medium'}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Predictions Grid */}
      {!loading && !error && predictions.length === 0 && mlApiAvailable && hotspots.length === 0 && (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
          <p className="text-gray-600">No prediction data available yet.</p>
          <p className="text-gray-500 text-sm mt-2">Predictions will appear once traffic data is processed.</p>
        </div>
      )}

      {!loading && predictions.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {predictions.map((pred) => {
            const currentCongestion = getCongestionLevel(pred.currentSpeed)
            const predicted30Min = pred.predictions.find(p => p.horizon === 30)
            const trend = predicted30Min ? getPredictionTrend(predicted30Min.change) : null

            return (
              <div key={pred.segmentId} className="bg-white rounded-lg shadow hover:shadow-lg transition p-6">
                {/* Header */}
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="font-semibold text-gray-800">{pred.segmentId}</h3>
                    <span className={`inline-block px-2 py-1 rounded text-xs font-medium mt-1 ${currentCongestion.color}`}>
                      {currentCongestion.label}
                    </span>
                  </div>
                  {trend && (
                    <div className={`flex items-center gap-1 ${trend.color}`}>
                      <trend.icon size={20} />
                      <span className="text-xs font-medium">{trend.label}</span>
                    </div>
                  )}
                </div>

                {/* Current Speed */}
                <div className="mb-4 pb-4 border-b">
                  <div className="text-sm text-gray-600">Current Speed</div>
                  <div className="text-2xl font-bold text-blue-600">{pred.currentSpeed.toFixed(1)} km/h</div>
                </div>

                {/* Predictions */}
                <div className="space-y-2">
                  <div className="text-sm font-medium text-gray-700 mb-2">Forecasts</div>
                  {pred.predictions.map((p) => (
                    <div key={p.horizon} className="flex items-center justify-between py-2 border-b border-gray-100 last:border-0">
                      <span className="text-sm text-gray-600">+{p.horizon} min</span>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-gray-800">{p.speed.toFixed(1)} km/h</span>
                        <span className={`text-xs ${
                          p.change > 0 ? 'text-green-600' : p.change < 0 ? 'text-red-600' : 'text-gray-500'
                        }`}>
                          ({p.change > 0 ? '+' : ''}{p.change.toFixed(1)}%)
                        </span>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Timestamp */}
                <div className="mt-4 pt-3 border-t text-xs text-gray-400">
                  Updated: {new Date(pred.timestamp).toLocaleTimeString()}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Info Footer */}
      <div className="mt-8 mb-8 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="font-semibold text-blue-900 mb-2">About ML Predictions</h3>
        <div className="text-sm text-blue-800 space-y-1">
          <p>• Predictions use XGBoost machine learning models trained on historical traffic patterns</p>
          <p>• Models consider time of day, day of week, historical averages, and current trends</p>
          <p>• Forecasts are generated for 10, 20, and 30 minutes ahead</p>
          <p>• Data refreshes automatically every 30 seconds</p>
        </div>
      </div>
    </div>
  )
}

export default Predict

