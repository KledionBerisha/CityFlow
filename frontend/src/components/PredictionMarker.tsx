/**
 * PredictionMarker Component
 * 
 * Displays ML prediction overlays on the map showing predicted traffic conditions
 */

import { Marker, Popup, Circle } from 'react-leaflet'
import { Icon } from 'leaflet'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'

interface PredictionMarkerProps {
  segmentId: string
  latitude: number
  longitude: number
  currentSpeed: number
  predictedSpeed: number
  horizon: number // minutes
  confidence?: number
  durationMinutes?: number
  expectedClearTime?: string
}

// Create custom icon for prediction markers
const predictionIcon = new Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-violet.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
})

function PredictionMarker({ 
  segmentId, 
  latitude, 
  longitude, 
  currentSpeed, 
  predictedSpeed, 
  horizon,
  confidence,
  durationMinutes,
  expectedClearTime
}: PredictionMarkerProps) {
  const speedChange = predictedSpeed - currentSpeed
  const percentChange = (speedChange / currentSpeed) * 100
  
  // Determine trend
  const getTrend = () => {
    if (speedChange > 5) return { icon: TrendingUp, label: 'Improving', color: 'text-green-600' }
    if (speedChange < -5) return { icon: TrendingDown, label: 'Worsening', color: 'text-red-600' }
    return { icon: Minus, label: 'Stable', color: 'text-gray-600' }
  }
  
  const trend = getTrend()
  
  // Color based on predicted congestion
  const getCongestionColor = (speed: number) => {
    if (speed > 50) return '#22c55e' // Green
    if (speed > 35) return '#eab308' // Yellow
    if (speed > 20) return '#ea580c' // Orange
    return '#dc2626' // Red
  }
  
  const circleColor = getCongestionColor(predictedSpeed)

  return (
    <>
      {/* Circle overlay showing prediction area */}
      <Circle
        center={[latitude, longitude]}
        radius={200}
        pathOptions={{
          color: circleColor,
          fillColor: circleColor,
          fillOpacity: 0.2,
          weight: 2,
          dashArray: '5, 5'
        }}
      />
      
      {/* Prediction marker */}
      <Marker 
        position={[latitude, longitude]} 
        icon={predictionIcon}
      >
        <Popup>
          <div className="p-2 min-w-[200px]">
            <div className="font-semibold text-gray-800 mb-2">{segmentId}</div>
            
            <div className="space-y-2 text-sm">
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Current:</span>
                <span className="font-medium">{currentSpeed.toFixed(1)} km/h</span>
              </div>
              
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Predicted ({horizon}min):</span>
                <span className="font-medium">{predictedSpeed.toFixed(1)} km/h</span>
              </div>
              
              <div className="flex justify-between items-center pt-2 border-t">
                <span className="text-gray-600">Change:</span>
                <span className={`font-medium ${
                  percentChange > 0 ? 'text-green-600' : percentChange < 0 ? 'text-red-600' : 'text-gray-600'
                }`}>
                  {percentChange > 0 ? '+' : ''}{percentChange.toFixed(1)}%
                </span>
              </div>
              
              <div className="flex items-center gap-2 pt-2 border-t">
                <trend.icon size={16} className={trend.color} />
                <span className={`text-sm font-medium ${trend.color}`}>{trend.label}</span>
              </div>
              
              {durationMinutes && (
                <div className="text-xs text-gray-600 pt-2 border-t font-medium">
                  ⏱ Duration: ~{durationMinutes} min
                  {expectedClearTime && (
                    <div className="text-gray-500 mt-1">
                      Clear by: {new Date(expectedClearTime).toLocaleTimeString()}
                    </div>
                  )}
                </div>
              )}
              {durationMinutes && (
                <div className="text-xs text-gray-600 pt-2 border-t font-medium">
                  ⏱ Duration: ~{durationMinutes} min
                  {expectedClearTime && (
                    <div className="text-gray-500 mt-1">
                      Clear by: {new Date(expectedClearTime).toLocaleTimeString()}
                    </div>
                  )}
                </div>
              )}
              {confidence && (
                <div className="text-xs text-gray-500 pt-2 border-t">
                  Confidence: {(confidence * 100).toFixed(0)}%
                </div>
              )}
            </div>
          </div>
        </Popup>
      </Marker>
    </>
  )
}

export default PredictionMarker
