import { CircleMarker, Popup } from 'react-leaflet'
import { TrafficReading } from '../services/api'

interface TrafficMarkerProps {
  reading: TrafficReading
}

function TrafficMarker({ reading }: TrafficMarkerProps) {
  if (!reading.latitude || !reading.longitude) {
    return null
  }

  // Determine color based on congestion level
  const getColor = (congestionLevel: string) => {
    switch (congestionLevel?.toUpperCase()) {
      case 'SEVERE':
        return '#dc2626' // red-600
      case 'HIGH':
        return '#ea580c' // orange-600
      case 'MODERATE':
        return '#eab308' // yellow-500
      case 'LOW':
        return '#22c55e' // green-500
      default:
        return '#6b7280' // gray-500
    }
  }

  const color = getColor(reading.congestionLevel)

  return (
    <CircleMarker
      center={[reading.latitude, reading.longitude]}
      radius={8}
      pathOptions={{
        fillColor: color,
        color: '#ffffff',
        weight: 2,
        opacity: 0.8,
        fillOpacity: 0.7,
      }}
    >
      <Popup>
        <div className="p-2">
          <strong>Sensor {reading.sensorId}</strong>
          <br />
          Speed: {reading.speedKmh?.toFixed(1)} km/h
          <br />
          Vehicles: {reading.vehicleCount}
          <br />
          Congestion: {reading.congestionLevel}
        </div>
      </Popup>
    </CircleMarker>
  )
}

export default TrafficMarker

