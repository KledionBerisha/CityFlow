import { Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { BusLocation } from '../services/api'

interface BusMarkerProps {
  bus: BusLocation
}

// Create custom bus icon with line number
const createBusIcon = (heading: number, lineNumber?: string) => {
  return L.divIcon({
    className: 'bus-marker',
    html: `
      <div style="
        transform: rotate(${heading}deg);
        width: 32px;
        height: 28px;
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
        border: 2px solid white;
        border-radius: 6px;
        box-shadow: 0 2px 6px rgba(0,0,0,0.4);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: bold;
        font-size: 11px;
        position: relative;
      ">
        ${lineNumber ? `<span style="font-size: 12px; font-weight: 700;">${lineNumber}</span>` : '🚌'}
      </div>
    `,
    iconSize: [32, 28],
    iconAnchor: [16, 14],
  })
}

function BusMarker({ bus }: BusMarkerProps) {
  if (!bus.latitude || !bus.longitude) {
    return null
  }

  return (
    <Marker
      position={[bus.latitude, bus.longitude]}
      icon={createBusIcon(bus.heading || 0, bus.lineNumber)}
    >
      <Popup>
        <div className="p-2 min-w-[180px]">
          {bus.lineNumber && (
            <div className="text-lg font-bold text-blue-600 mb-1">
              Line {bus.lineNumber}
            </div>
          )}
          {bus.routeName && (
            <div className="text-xs text-gray-500 mb-2 border-b pb-2">
              {bus.routeName}
            </div>
          )}
          <div className="space-y-1 text-sm">
            <div><strong>Vehicle:</strong> {bus.vehicleId}</div>
            <div><strong>Speed:</strong> {bus.speedKmh?.toFixed(1)} km/h</div>
            <div><strong>Passengers:</strong> ~{bus.occupancy || 0}</div>
            {bus.nextStopId && (
              <div className="mt-2 pt-2 border-t">
                <strong>Next Stop:</strong>
                <div className="text-blue-600">{bus.nextStopId}</div>
                {bus.distanceToNextStopKm !== undefined && (
                  <div className="text-xs text-gray-500">
                    {(bus.distanceToNextStopKm * 1000).toFixed(0)}m away
                    {bus.estimatedArrivalSeconds !== undefined && (
                      <> • ~{Math.ceil(bus.estimatedArrivalSeconds / 60)} min</>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </Popup>
    </Marker>
  )
}

export default BusMarker

