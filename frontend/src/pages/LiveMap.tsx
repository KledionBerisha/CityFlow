import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, useMap, GeoJSON } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import DataPanel from '../components/DataPanel'
import LocationPanel from '../components/LocationPanel'
import BusMarker from '../components/BusMarker'
import TrafficMarker from '../components/TrafficMarker'
import { CarMarker } from '../components/CarMarker'
import PredictionMarker from '../components/PredictionMarker'
import { useMapData } from '../hooks/useMapData'
import { useBusLocations } from '../hooks/useBusLocations'
import { useTrafficReadings } from '../hooks/useTrafficReadings'
import { useRoadOverlays } from '../hooks/useRoadOverlays'
import { useCarLocations } from '../hooks/useCarLocations'
import { getRoadTrafficData, RoadTrafficData, getCongestionHotspots } from '../services/api'

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
  const [showBuses, setShowBuses] = useState(true)
  const [showTraffic, setShowTraffic] = useState(true)
  const [congestionHotspots, setCongestionHotspots] = useState<any[]>([])
  const [showPredictions, setShowPredictions] = useState(true)
  
  // Fetch congestion hotspots for predictions
  useEffect(() => {
    const fetchHotspots = async () => {
      try {
        const hotspots = await getCongestionHotspots()
        setCongestionHotspots(Array.isArray(hotspots) ? hotspots : [])
      } catch (error) {
        console.error('Error fetching congestion hotspots:', error)
        setCongestionHotspots([])
      }
    }
    
    fetchHotspots()
    const interval = setInterval(fetchHotspots, 30000) // Refresh every 30s
    return () => clearInterval(interval)
  }, [])

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
        {showBuses && busLocations.map((bus) => (
          <BusMarker key={bus.busId} bus={bus} />
        ))}
        
        {/* Traffic Markers */}
        {showTraffic && trafficReadings.map((reading) => (
          <TrafficMarker key={reading.sensorId} reading={reading} />
        ))}
        
        {/* Prediction Markers - Congestion Hotspots */}
        {showPredictions && congestionHotspots.slice(0, 10).map((hotspot) => (
          <PredictionMarker
            key={hotspot.road_segment_id}
            segmentId={hotspot.road_segment_id}
            latitude={hotspot.latitude}
            longitude={hotspot.longitude}
            currentSpeed={hotspot.current_speed_kmh}
            predictedSpeed={hotspot.predicted_speed_kmh}
            horizon={hotspot.prediction_horizon_minutes}
            durationMinutes={hotspot.duration_prediction?.predicted_duration_minutes}
            expectedClearTime={hotspot.duration_prediction?.expected_clear_time}
          />
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

      {/* Layer Controls - Top Left */}
      <div className="absolute top-4 left-4 z-[1000] bg-white rounded-lg shadow-lg p-4 min-w-[220px]">
        <div className="font-semibold text-sm text-gray-800 mb-3 pb-2 border-b border-gray-200">Layer Controls</div>
        
        <div className="space-y-2.5">
          {/* Cars Toggle */}
          <label className="flex items-center gap-2.5 cursor-pointer hover:bg-gray-50 -mx-2 px-2 py-1.5 rounded transition-colors">
            <input
              type="checkbox"
              checked={showCars}
              onChange={(e) => setShowCars(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0 cursor-pointer"
            />
            <span className="text-sm font-medium text-gray-700 flex items-center gap-1.5 select-none">
              🚗 <span>Cars ({carLocations.length})</span>
            </span>
          </label>

          {/* Buses Toggle */}
          <label className="flex items-center gap-2.5 cursor-pointer hover:bg-gray-50 -mx-2 px-2 py-1.5 rounded transition-colors">
            <input
              type="checkbox"
              checked={showBuses}
              onChange={(e) => setShowBuses(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0 cursor-pointer"
            />
            <span className="text-sm font-medium text-gray-700 flex items-center gap-1.5 select-none">
              🚌 <span>Buses ({busLocations.length})</span>
            </span>
          </label>

          {/* Traffic Sensors Toggle */}
          <label className="flex items-center gap-2.5 cursor-pointer hover:bg-gray-50 -mx-2 px-2 py-1.5 rounded transition-colors">
            <input
              type="checkbox"
              checked={showTraffic}
              onChange={(e) => setShowTraffic(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0 cursor-pointer"
            />
            <span className="text-sm font-medium text-gray-700 flex items-center gap-1.5 select-none">
              📊 <span>Traffic Sensors ({trafficReadings.length})</span>
            </span>
          </label>

          {/* Predictions Toggle */}
          <label className="flex items-center gap-2.5 cursor-pointer hover:bg-gray-50 -mx-2 px-2 py-1.5 rounded transition-colors">
            <input
              type="checkbox"
              checked={showPredictions}
              onChange={(e) => setShowPredictions(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-2 focus:ring-blue-500 focus:ring-offset-0 cursor-pointer"
            />
            <span className="text-sm font-medium text-gray-700 select-none">
              📍 Predictions
            </span>
          </label>
        </div>
        
        {congestionHotspots.length > 0 && showPredictions && (
          <div className="text-xs text-gray-500 pt-3 mt-3 border-t border-gray-200">
            {congestionHotspots.length} congestion hotspot{congestionHotspots.length !== 1 ? 's' : ''} detected
          </div>
        )}
      </div>

      {/* Location Panel - Bottom Right */}
      <div className="absolute bottom-4 right-4 z-[1000]">
        <LocationPanel location={location} />
      </div>
    </div>
  )
}

export default LiveMap

