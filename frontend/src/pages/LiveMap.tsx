import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, useMap, GeoJSON } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import DataPanel from '../components/DataPanel'
import LocationPanel from '../components/LocationPanel'
import BusMarker from '../components/BusMarker'
import TrafficMarker from '../components/TrafficMarker'
import { useMapData } from '../hooks/useMapData'
import { useBusLocations } from '../hooks/useBusLocations'
import { useTrafficReadings } from '../hooks/useTrafficReadings'
import { getRoadTrafficData, RoadTrafficData } from '../services/api'

// Fix for default marker icons in React Leaflet
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
})

// Component to handle map bounds and styling
function MapController() {
  const map = useMap()
  
  useEffect(() => {
    // Set initial view to Prishtina, Kosovo
    map.setView([42.6629, 21.1655], 12)
    
    // Style the map to match the design (light beige background)
    const style = document.createElement('style')
    style.textContent = `
      .leaflet-container {
        background-color: #f5f5f0 !important;
      }
      .leaflet-tile-pane {
        opacity: 0.7;
      }
    `
    document.head.appendChild(style)
    
    return () => {
      if (document.head.contains(style)) {
        document.head.removeChild(style)
      }
    }
  }, [map])
  
  return null
}

// Component to render road overlays with traffic-based colors
function RoadOverlays() {
  const [roadData, setRoadData] = useState<RoadTrafficData[]>([])

  useEffect(() => {
    const fetchRoads = async () => {
      try {
        const data = await getRoadTrafficData()
        setRoadData(data)
      } catch (error) {
        console.error('Error fetching road traffic data:', error)
      }
    }

    fetchRoads()
    const interval = setInterval(fetchRoads, 10000) // Update every 10 seconds
    
    return () => clearInterval(interval)
  }, [])

  if (!roadData || roadData.length === 0) {
    return null
  }

  // Style function for roads based on traffic
  const getRoadStyle = (road: RoadTrafficData) => {
    let color = '#FFD700' // Default yellow
    let weight = 4

    switch (road.congestionLevel) {
      case 'SEVERE':
        color = '#dc2626' // Red for heavy traffic
        weight = 6
        break
      case 'HIGH':
        color = '#ea580c' // Orange
        weight = 5
        break
      case 'MODERATE':
        color = '#eab308' // Yellow
        weight = 4
        break
      case 'LOW':
        color = '#22c55e' // Green
        weight = 3
        break
      default:
        color = '#808080' // Gray
        weight = 3
    }

    return {
      color,
      weight,
      opacity: 0.9,
      fillColor: color,
      fillOpacity: 0.3,
    }
  }

  return (
    <>
      {roadData.map((road) => (
        <GeoJSON
          key={road.roadId}
          data={{
            type: 'Feature',
            geometry: road.geometry,
            properties: {
              name: road.roadName,
              congestionLevel: road.congestionLevel,
              averageSpeed: road.averageSpeed,
              vehicleCount: road.vehicleCount,
            },
          }}
          style={() => getRoadStyle(road)}
        />
      ))}
    </>
  )
}

function LiveMap() {
  const { currentTime, vehicleCount, accidents, location } = useMapData()
  const { busLocations } = useBusLocations()
  const { trafficReadings } = useTrafficReadings()

  return (
    <div className="relative w-full h-full">
      <MapContainer
        center={[42.6629, 21.1655]}
        zoom={12}
        style={{ height: '100%', width: '100%' }}
        zoomControl={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapController />
        <RoadOverlays />
        
        {/* Bus Markers */}
        {busLocations.map((bus) => (
          <BusMarker key={bus.busId} bus={bus} />
        ))}
        
        {/* Traffic Markers */}
        {trafficReadings.map((reading) => (
          <TrafficMarker key={reading.sensorId} reading={reading} />
        ))}
      </MapContainer>

      {/* Data Panels - Top Right */}
      <div className="absolute top-4 right-4 z-[1000] flex flex-col gap-3">
        <DataPanel
          title="Current Time"
          value={currentTime}
          subtitle={null}
          stats={null}
        />
        <DataPanel
          title="Vehicle Count"
          value={vehicleCount.avg.toString()}
          subtitle="AVG"
          stats={{
            max: vehicleCount.max,
            min: vehicleCount.min,
          }}
        />
        <DataPanel
          title="Accidents"
          value={accidents.count.toString()}
          subtitle="/ 24Hrs"
          stats={null}
        />
      </div>

      {/* Location Panel - Bottom Right */}
      <div className="absolute bottom-4 right-4 z-[1000]">
        <LocationPanel location={location} />
      </div>
    </div>
  )
}

export default LiveMap

