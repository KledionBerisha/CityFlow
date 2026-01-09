import { useState, useEffect } from 'react'
import { getRoadOverlays } from '../services/api'

/**
 * Hook to fetch and return road overlay data for the map
 * TODO: Connect to backend endpoint that returns GeoJSON road data
 */
export function useRoadOverlays() {
  const [roadData, setRoadData] = useState<any[]>([])

  useEffect(() => {
    const fetchRoads = async () => {
      try {
        // TODO: Replace with actual API call when backend is ready
        const data = await getRoadOverlays()
        setRoadData(data)
      } catch (error) {
        console.error('Error fetching road overlays:', error)
        setRoadData([])
      }
    }

    fetchRoads()
    
    // Optionally refresh road data periodically
    const interval = setInterval(fetchRoads, 60000) // Every minute
    
    return () => clearInterval(interval)
  }, [])

  return roadData
}

