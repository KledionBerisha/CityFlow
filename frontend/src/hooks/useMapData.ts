import { useState, useEffect } from 'react'
import * as api from '../services/api'

interface VehicleCount {
  avg: number
  max: number
  min: number
}

interface MapData {
  currentTime: string
  vehicleCount: VehicleCount
  accidents: { count: number }
  location: string
}

export function useMapData(): MapData {
  const [currentTime, setCurrentTime] = useState<string>('00:00:00')
  const [vehicleCount, setVehicleCount] = useState<VehicleCount>({
    avg: 0,
    max: 0,
    min: 0,
  })
  const [accidents, setAccidents] = useState<{ count: number }>({ count: 0 })
  const [location, setLocation] = useState<string>('Prishtina')

  useEffect(() => {
    // Update time every second
    const timeInterval = setInterval(() => {
      const now = new Date()
      setCurrentTime(now.toLocaleTimeString('en-US', { hour12: false }))
    }, 1000)

    // Fetch initial data
    const fetchData = async () => {
      try {
        // TODO: Replace with actual API calls when backend is connected
        const vehicleData = await api.getVehicleCount()
        const accidentData = await api.getAccidentCount()
        const locationData = await api.getCurrentLocation()

        setVehicleCount(vehicleData)
        setAccidents(accidentData)
        if (locationData) {
          setLocation(locationData)
        }
      } catch (error) {
        console.error('Error fetching map data:', error)
        // Use placeholder data on error
        setVehicleCount({ avg: 173, max: 371, min: 87 })
        setAccidents({ count: 2 })
      }
    }

    fetchData()

    // Poll for updates every 5 seconds
    const dataInterval = setInterval(fetchData, 5000)

    return () => {
      clearInterval(timeInterval)
      clearInterval(dataInterval)
    }
  }, [])

  return {
    currentTime,
    vehicleCount,
    accidents,
    location,
  }
}

