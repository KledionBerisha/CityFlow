/**
 * API Service Layer
 * 
 * This file contains all API functions that connect to the backend.
 * Replace the placeholder implementations with actual fetch calls when backend is ready.
 * 
 * Base URL: http://localhost:8000/api (via Vite proxy)
 */

const API_BASE_URL = '/api'

// ============================================================================
// Vehicle/Traffic Count APIs
// ============================================================================

export interface VehicleCountResponse {
  avg: number
  max: number
  min: number
}

/**
 * Get current vehicle count statistics
 * TODO: Connect to backend endpoint (e.g., GET /api/traffic/vehicle-count)
 */
export async function getVehicleCount(): Promise<VehicleCountResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/traffic/vehicle-count`)
    if (!response.ok) {
      throw new Error('Failed to fetch vehicle count')
    }
    return await response.json()
  } catch (error) {
    // Placeholder data for development
    console.warn('Using placeholder vehicle count data:', error)
    return { avg: 173, max: 371, min: 87 }
  }
}

// ============================================================================
// Accident/Incident APIs
// ============================================================================

export interface AccidentCountResponse {
  count: number
}

/**
 * Get accident count for the last 24 hours
 * TODO: Connect to backend endpoint (e.g., GET /api/incidents/count?hours=24)
 */
export async function getAccidentCount(): Promise<AccidentCountResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/incidents/count?hours=24`)
    if (!response.ok) {
      throw new Error('Failed to fetch accident count')
    }
    return await response.json()
  } catch (error) {
    // Placeholder data for development
    console.warn('Using placeholder accident count data:', error)
    return { count: 2 }
  }
}

/**
 * Get all incidents
 * TODO: Connect to backend endpoint (e.g., GET /api/incidents)
 */
export async function getIncidents() {
  try {
    const response = await fetch(`${API_BASE_URL}/incidents`)
    if (!response.ok) {
      throw new Error('Failed to fetch incidents')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching incidents:', error)
    return []
  }
}

// ============================================================================
// Location APIs
// ============================================================================

/**
 * Get current location/city being monitored
 * TODO: Connect to backend endpoint (e.g., GET /api/location/current)
 */
export async function getCurrentLocation(): Promise<string | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/location/current`)
    if (!response.ok) {
      throw new Error('Failed to fetch location')
    }
    const data = await response.json()
    return data.location || data.name || null
  } catch (error) {
    // Placeholder data for development
    console.warn('Using placeholder location data:', error)
    return 'Prishtina'
  }
}

// ============================================================================
// Bus Location APIs
// ============================================================================

export interface BusLocation {
  busId: string
  vehicleId: string
  routeId: string
  latitude: number
  longitude: number
  speedKmh: number
  heading: number
  timestamp: string
  occupancy: number
  source: string
  nextStopId?: string
  distanceToNextStopKm?: number
  estimatedArrivalSeconds?: number
}

/**
 * Get all current bus locations
 * TODO: Already available at GET /api/bus-locations/current
 */
export async function getBusLocations(): Promise<BusLocation[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/bus-locations/current`)
    if (!response.ok) {
      throw new Error('Failed to fetch bus locations')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching bus locations:', error)
    return []
  }
}

/**
 * Get bus locations for a specific route
 * TODO: Already available at GET /api/bus-locations/current/route/{routeId}
 */
export async function getBusLocationsByRoute(routeId: string): Promise<BusLocation[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/bus-locations/current/route/${routeId}`)
    if (!response.ok) {
      throw new Error('Failed to fetch bus locations for route')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching bus locations by route:', error)
    return []
  }
}

// ============================================================================
// Route APIs
// ============================================================================

export interface Route {
  id: string
  routeCode: string
  routeName: string
  isActive: boolean
}

/**
 * Get all routes
 * TODO: Already available at GET /api/routes
 */
export async function getRoutes(): Promise<Route[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/routes`)
    if (!response.ok) {
      throw new Error('Failed to fetch routes')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching routes:', error)
    return []
  }
}

// ============================================================================
// Traffic Data APIs
// ============================================================================

export interface TrafficReading {
  sensorId: string
  latitude: number
  longitude: number
  speedKmh: number
  vehicleCount: number
  congestionLevel: string
  timestamp: string
}

/**
 * Get current traffic readings
 * TODO: Connect to backend endpoint (e.g., GET /api/traffic/readings/current)
 */
export async function getTrafficReadings(): Promise<TrafficReading[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/traffic/readings/current`)
    if (!response.ok) {
      throw new Error('Failed to fetch traffic readings')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching traffic readings:', error)
    return []
  }
}

// ============================================================================
// Road/Map Overlay APIs
// ============================================================================

export interface RoadTrafficData {
  roadId: string
  roadName?: string
  geometry: {
    type: 'LineString'
    coordinates: number[][]
  }
  congestionLevel: 'LOW' | 'MODERATE' | 'HIGH' | 'SEVERE'
  averageSpeed: number
  vehicleCount: number
}

/**
 * Get road overlay data (GeoJSON format)
 * TODO: Connect to backend endpoint (e.g., GET /api/map/roads)
 */
export async function getRoadOverlays(): Promise<any[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/map/roads`)
    if (!response.ok) {
      throw new Error('Failed to fetch road overlays')
    }
    const data = await response.json()
    // Expected format: Array of GeoJSON Feature objects
    return data.features || data || []
  } catch (error) {
    console.error('Error fetching road overlays:', error)
    return []
  }
}

/**
 * Get road traffic data with congestion levels
 * TODO: Connect to backend endpoint (e.g., GET /api/traffic/roads)
 */
export async function getRoadTrafficData(): Promise<RoadTrafficData[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/traffic/roads`)
    if (!response.ok) {
      throw new Error('Failed to fetch road traffic data')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching road traffic data:', error)
    return []
  }
}

// ============================================================================
// Analytics/Prediction APIs (ML Service)
// ============================================================================

export interface TrafficPrediction {
  location: { lat: number; lng: number }
  predictedSpeed: number
  predictedCongestion: string
  confidence: number
  timeWindow: string
}

export interface MLPredictionRequest {
  readings: {
    road_segment_id: string
    timestamp: string
    speed_kmh: number
    vehicle_count?: number
    latitude?: number
    longitude?: number
  }[]
  prediction_horizons: number[] // Minutes ahead: [10, 20, 30]
}

export interface MLPredictionResponse {
  predictions: {
    road_segment_id: string
    current_speed_kmh: number
    predicted_speed_kmh: number
    prediction_horizon_minutes: number
    confidence: number | null
    timestamp: string
  }[]
  model_version: string
  timestamp: string
}

/**
 * Get ML-based traffic speed predictions
 * Connects to ML API: POST http://localhost:8090/predict
 */
export async function getMLPredictions(request: MLPredictionRequest): Promise<MLPredictionResponse | null> {
  try {
    const response = await fetch('http://localhost:8090/predict', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
    
    if (!response.ok) {
      throw new Error(`ML API error: ${response.status}`)
    }
    
    return await response.json()
  } catch (error) {
    console.error('Error fetching ML predictions:', error)
    return null
  }
}

/**
 * Get predictions for all road segments
 * Connects to ML API: GET http://localhost:8090/predict/all?horizon=30
 */
export async function getAllSegmentPredictions(horizon: number = 30): Promise<MLPredictionResponse | null> {
  try {
    const response = await fetch(`http://localhost:8090/predict/all?horizon=${horizon}`)
    
    if (!response.ok) {
      throw new Error(`ML API error: ${response.status}`)
    }
    
    return await response.json()
  } catch (error) {
    console.error('Error fetching all segment predictions:', error)
    return null
  }
}

/**
 * Check ML API health
 */
export async function checkMLAPIHealth(): Promise<boolean> {
  try {
    const response = await fetch('http://localhost:8090/health')
    return response.ok
  } catch (error) {
    return false
  }
}

/**
 * Get traffic predictions (Legacy - for backward compatibility)
 * TODO: Connect to backend endpoint (e.g., GET /api/analytics/predictions)
 */
export async function getTrafficPredictions(): Promise<TrafficPrediction[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/analytics/predictions`)
    if (!response.ok) {
      throw new Error('Failed to fetch traffic predictions')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching traffic predictions:', error)
    return []
  }
}

