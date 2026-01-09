import { useState, useEffect } from 'react'
import { getBusLocations, BusLocation } from '../services/api'

export function useBusLocations() {
  const [busLocations, setBusLocations] = useState<BusLocation[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchBusLocations = async () => {
      try {
        const locations = await getBusLocations()
        setBusLocations(locations)
      } catch (error) {
        console.error('Error fetching bus locations:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchBusLocations()
    
    // Update every 5 seconds for real-time movement
    const interval = setInterval(fetchBusLocations, 5000)

    return () => clearInterval(interval)
  }, [])

  return { busLocations, loading }
}

