import { useState, useEffect } from 'react'
import { getCarLocations, CarLocation } from '../services/api'

export function useCarLocations() {
  const [carLocations, setCarLocations] = useState<CarLocation[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchCarLocations = async () => {
      try {
        const locations = await getCarLocations()
        setCarLocations(locations)
      } catch (error) {
        console.error('Error fetching car locations:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchCarLocations()
    
    // Update every 3 seconds for real-time movement (cars update faster than buses)
    const interval = setInterval(fetchCarLocations, 3000)

    return () => clearInterval(interval)
  }, [])

  return { carLocations, loading }
}
