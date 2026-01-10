import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, useMap, GeoJSON } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import DataPanel from '../components/DataPanel'
import LocationPanel from '../components/LocationPanel'
import BusMarker from '../components/BusMarker'
import TrafficMarker from '../components/TrafficMarker'
import { CarMarker } from '../components/CarMarker'
import { useMapData } from '../hooks/useMapData'
import { useBusLocations } from '../hooks/useBusLocations'
import { useTrafficReadings } from '../hooks/useTrafficReadings'
import { useRoadOverlays } from '../hooks/useRoadOverlays'
import { useCarLocations } from '../hooks/useCarLocations'
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

// Helper function to validate GeoJSON geometry
function isValidGeometry(geometry: any): boolean {
  if (!geometry || !geometry.type || !geometry.coordinates) {
    return false
  }
  
  // Check that coordinates is an array
  if (!Array.isArray(geometry.coordinates)) {
    return false
  }
  
  // LineString must have at least 2 points with valid numbers
  if (geometry.type === 'LineString') {
    if (geometry.coordinates.length < 2) {
      return false
    }
    // Validate each coordinate pair has valid numbers
    return geometry.coordinates.every((coord: any) => 
      Array.isArray(coord) && 
      coord.length >= 2 && 
      typeof coord[0] === 'number' && 
      typeof coord[1] === 'number' &&
      !isNaN(coord[0]) && 
      !isNaN(coord[1])
    )
  }
  
  // Point must have valid coordinates
  if (geometry.type === 'Point') {
    return geometry.coordinates.length >= 2 &&
      typeof geometry.coordinates[0] === 'number' &&
      typeof geometry.coordinates[1] === 'number' &&
      !isNaN(geometry.coordinates[0]) &&
      !isNaN(geometry.coordinates[1])
  }
  
  return false
}

// Style function for roads based on traffic
function getRoadStyle(congestionLevel: string) {
  let color = '#FFD700' // Default yellow
  let weight = 4

  switch (congestionLevel) {
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

// Component to render road overlays with traffic-based colors
function RoadOverlays() {
  const basicRoadData = useRoadOverlays()
  const [trafficRoadData, setTrafficRoadData] = useState<RoadTrafficData[]>([])

  // Try to fetch traffic-based road data
  useEffect(() => {
    const fetchTrafficRoads = async () => {
      try {
        const data = await getRoadTrafficData()
        if (data && data.length > 0) {
          setTrafficRoadData(data)
        }
      } catch (error) {
        // Silently fail - will use basic road data instead
        console.debug('Traffic road data not available, using basic overlays')
      }
    }

    fetchTrafficRoads()
    const interval = setInterval(fetchTrafficRoads, 10000) // Update every 10 seconds
    
    return () => clearInterval(interval)
  }, [])

  // Filter out roads with invalid geometries
  const validTrafficRoads = trafficRoadData.filter(road => isValidGeometry(road.geometry))

  // If we have valid traffic data, use it with colors
  if (validTrafficRoads.length > 0) {
    return (
      <>
        {validTrafficRoads.map((road) => (
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
            style={() => getRoadStyle(road.congestionLevel)}
          />
        ))}
      </>
    )
  }

  // Fallback to basic road overlays (yellow)
  if (!basicRoadData || basicRoadData.length === 0) {
    return null
  }
  
  // Filter basic road data as well
  const validBasicRoads = basicRoadData.filter((road: any) => {
    if (!road) return false
    // If it's a Feature, check the geometry
    if (road.type === 'Feature') {
      return isValidGeometry(road.geometry)
    }
    // If it's a geometry directly
    return isValidGeometry(road)
  })
  
  if (validBasicRoads.length === 0) {
    return null
  }
  
  const basicRoadStyle = () => ({
    color: '#FFD700', // Yellow outline
    weight: 4,
    opacity: 1,
    fillColor: '#808080', // Gray fill
    fillOpacity: 0.6,
  })
  
  return (
    <>
      {validBasicRoads.map((road: any, index: number) => (
        <GeoJSON
          key={index}
          data={road}
          style={basicRoadStyle}
        />
      ))}
    </>
  )
}

function LiveMap() {
  const { currentTime, vehicleCount, accidents, location } = useMapData()
  const { busLocations } = useBusLocations()
  const { trafficReadings } = useTrafficReadings()
  const { carLocations } = useCarLocations()
  const [showCars, setShowCars] = useState(true)

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
        
        {/* Car Markers - Traffic Flow */}
        {showCars && carLocations.map((car) => (
          <CarMarker key={car.carId} location={car} />
        ))}
        
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

      {/* Layer Controls - Top Left */}
      <div className="absolute top-4 left-14 z-[1000] bg-white rounded-lg shadow p-2">
        <label className="flex items-center gap-2 cursor-pointer text-sm">
          <input
            type="checkbox"
            checked={showCars}
            onChange={(e) => setShowCars(e.target.checked)}
            className="w-4 h-4"
          />
          <span>🚗 Cars ({carLocations.length})</span>
        </label>
      </div>
    </div>
  )
}

export default LiveMap

