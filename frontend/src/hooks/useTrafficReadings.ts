import { useState, useEffect } from 'react'
import { getTrafficReadings, TrafficReading } from '../services/api'

export function useTrafficReadings() {
  const [trafficReadings, setTrafficReadings] = useState<TrafficReading[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchTrafficReadings = async () => {
      try {
        const readings = await getTrafficReadings()
        setTrafficReadings(readings)
      } catch (error) {
        console.error('Error fetching traffic readings:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchTrafficReadings()
    
    // Update every 5 seconds
    const interval = setInterval(fetchTrafficReadings, 5000)

    return () => clearInterval(interval)
  }, [])

  return { trafficReadings, loading }
}

