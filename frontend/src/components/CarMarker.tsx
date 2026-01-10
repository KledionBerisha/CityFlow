import { Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { CarLocation } from '../services/api'

interface CarMarkerProps {
  location: CarLocation
}

// Create a car icon
const carIcon = L.divIcon({
  className: 'car-marker',
  html: `<div class="car-icon">🚗</div>`,
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  popupAnchor: [0, -10],
})

function getSpeedColor(speedKmh: number): string {
  if (speedKmh > 50) return '#22c55e' // Green - fast
  if (speedKmh > 30) return '#eab308' // Yellow - moderate
  if (speedKmh > 15) return '#f97316' // Orange - slow
  return '#ef4444' // Red - very slow / congested
}

function getCongestionLabel(level: number): string {
  if (level < 0.2) return 'Free Flow'
  if (level < 0.4) return 'Light Traffic'
  if (level < 0.6) return 'Moderate'
  if (level < 0.8) return 'Heavy'
  return 'Congested'
}

export function CarMarker({ location }: CarMarkerProps) {
  const speedColor = getSpeedColor(location.speedKmh)
  const congestionLabel = getCongestionLabel(location.congestionLevel)
  const roadName = (location as any).roadName || 'Unknown Road'

  return (
    <Marker 
      position={[location.latitude, location.longitude]}
      icon={carIcon}
    >
      <Popup>
        <div className="car-popup">
          <h4 className="font-bold text-sm mb-2">🚗 {location.vehicleId}</h4>
          <div className="text-xs space-y-1">
            <div className="flex justify-between gap-2">
              <span className="text-gray-500">Road:</span>
              <span className="font-medium text-right">{roadName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Speed:</span>
              <span style={{ color: speedColor }} className="font-medium">
                {location.speedKmh.toFixed(1)} km/h
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Traffic:</span>
              <span className="font-medium">{congestionLabel}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Heading:</span>
              <span>{location.heading.toFixed(0)}°</span>
            </div>
          </div>
        </div>
      </Popup>
    </Marker>
  )
}
