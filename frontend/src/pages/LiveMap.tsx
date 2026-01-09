import { useEffect } from 'react'
import { MapContainer, TileLayer, useMap, GeoJSON } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import DataPanel from '../components/DataPanel'
import LocationPanel from '../components/LocationPanel'
import { useMapData } from '../hooks/useMapData'
import { useRoadOverlays } from '../hooks/useRoadOverlays'

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

// Component to render road overlays
function RoadOverlays() {
  const roadData = useRoadOverlays()
  
  if (!roadData || roadData.length === 0) {
    return null
  }
  
  // Style function for roads - yellow outline with gray fill
  const roadStyle = () => ({
    color: '#FFD700', // Yellow outline
    weight: 4,
    opacity: 1,
    fillColor: '#808080', // Gray fill
    fillOpacity: 0.6,
  })
  
  return (
    <>
      {roadData.map((road, index) => (
        <GeoJSON
          key={index}
          data={road}
          style={roadStyle}
        />
      ))}
    </>
  )
}

function LiveMap() {
  const { currentTime, vehicleCount, accidents, location } = useMapData()

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

