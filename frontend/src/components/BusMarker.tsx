import { Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { BusLocation } from '../services/api'

interface BusMarkerProps {
  bus: BusLocation
}

// Create custom bus icon
const createBusIcon = (heading: number) => {
  return L.divIcon({
    className: 'bus-marker',
    html: `
      <div style="
        transform: rotate(${heading}deg);
        width: 24px;
        height: 24px;
        background: #2563eb;
        border: 2px solid white;
        border-radius: 4px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: bold;
        font-size: 10px;
      ">
        🚌
      </div>
    `,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  })
}

function BusMarker({ bus }: BusMarkerProps) {
  if (!bus.latitude || !bus.longitude) {
    return null
  }

  return (
    <Marker
      position={[bus.latitude, bus.longitude]}
      icon={createBusIcon(bus.heading || 0)}
    >
      <Popup>
        <div className="p-2">
          <strong>Bus {bus.vehicleId}</strong>
          <br />
          Speed: {bus.speedKmh?.toFixed(1)} km/h
          <br />
          Occupancy: {bus.occupancy || 0}%
          {bus.nextStopId && (
            <>
              <br />
              Next Stop: {bus.nextStopId}
            </>
          )}
        </div>
      </Popup>
    </Marker>
  )
}

export default BusMarker

