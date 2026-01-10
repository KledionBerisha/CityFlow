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
 * Computes from traffic sensor readings
 */
export async function getVehicleCount(): Promise<VehicleCountResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/traffic/current`)
    if (!response.ok) {
      throw new Error('Failed to fetch traffic data')
    }
    const readings = await response.json()
    if (!Array.isArray(readings) || readings.length === 0) {
      return { avg: 0, max: 0, min: 0 }
    }
    const counts = readings.map((r: any) => r.vehicleCount || 0)
    const sum = counts.reduce((a: number, b: number) => a + b, 0)
    return {
      avg: Math.round(sum / counts.length),
      max: Math.max(...counts),
      min: Math.min(...counts),
    }
  } catch (error) {
    // Placeholder data for development
    console.warn('Using placeholder vehicle count data:', error)
    return { avg: 0, max: 0, min: 0 }
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
 * Uses the /api/incidents/recent endpoint and counts the results
 */
export async function getAccidentCount(): Promise<AccidentCountResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/incidents/recent?hoursBack=24`)
    if (!response.ok) {
      throw new Error('Failed to fetch incidents')
    }
    const incidents = await response.json()
    return { count: Array.isArray(incidents) ? incidents.length : 0 }
  } catch (error) {
    // Placeholder data for development  
    console.warn('Using placeholder accident count data:', error)
    return { count: 0 }
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
 * Currently returns static value - no backend endpoint exists for this yet
 */
export async function getCurrentLocation(): Promise<string | null> {
  // Static location - Prishtina, Kosovo
  // TODO: Implement backend endpoint if dynamic location support is needed
  return 'Prishtina'
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
  lineNumber?: string   // e.g., "1", "1A", "3", "4"
  routeName?: string    // e.g., "Linja 1: Qendra - Veternik"
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
// Car Location APIs
// ============================================================================

export interface CarLocation {
  carId: string
  vehicleId: string
  latitude: number
  longitude: number
  speedKmh: number
  heading: number
  timestamp: string
  trafficDensity: number
  congestionLevel: number
}

/**
 * Get all current car locations
 * Available at GET /api/car-locations/current
 */
export async function getCarLocations(): Promise<CarLocation[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/car-locations/current`)
    if (!response.ok) {
      throw new Error('Failed to fetch car locations')
    }
    return await response.json()
  } catch (error) {
    console.error('Error fetching car locations:', error)
    return []
  }
}

// ============================================================================
// Congestion Duration Prediction APIs
// ============================================================================

export interface CongestionDurationRequest {
  road_segment_id: string
  current_speed_kmh: number
  normal_speed_kmh: number
  current_congestion_level: number
  vehicle_count?: number
  latitude?: number
  longitude?: number
}

export interface CongestionDurationResponse {
  road_segment_id: string
  current_congestion_level: number
  predicted_duration_minutes: number
  confidence: 'low' | 'medium' | 'high'
  expected_clear_time: string
  prediction_factors: {
    peak_hour: boolean
    weekend: boolean
    congestion_severity: number
    time_factor: number
    weekend_factor: number
    minutes_to_peak_end?: number
  }
  timestamp: string
}

/**
 * Predict how long congestion will last on a road segment
 * Uses ML model to estimate duration based on current conditions
 */
export async function predictCongestionDuration(
  request: CongestionDurationRequest
): Promise<CongestionDurationResponse | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/ml/predict/congestion-duration`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
    if (!response.ok) {
      throw new Error('Failed to predict congestion duration')
    }
    return await response.json()
  } catch (error) {
    console.error('Error predicting congestion duration:', error)
    return null
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
  sensorName?: string
  roadSegmentId?: string
  latitude: number
  longitude: number
  speedKmh: number
  vehicleCount: number
  congestionLevel: string
  timestamp: string
}

/**
 * Get current traffic readings
 * Backend endpoint: GET /api/traffic/current
 */
export async function getTrafficReadings(): Promise<TrafficReading[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/traffic/current`)
    if (!response.ok) {
      throw new Error('Failed to fetch traffic readings')
    }
    // Map backend response to frontend format
    const data = await response.json()
    return data.map((reading: any) => ({
      sensorId: reading.sensorCode || reading.sensorId,
      latitude: reading.latitude || getSensorLocation(reading.sensorCode).lat,
      longitude: reading.longitude || getSensorLocation(reading.sensorCode).lng,
      speedKmh: reading.averageSpeed,
      vehicleCount: reading.vehicleCount,
      congestionLevel: mapCongestionLevel(reading.congestionLevel),
      timestamp: reading.timestamp,
    }))
  } catch (error) {
    console.error('Error fetching traffic readings:', error)
    return []
  }
}

// Map backend congestion levels to frontend format
function mapCongestionLevel(level: string): string {
  const mapping: Record<string, string> = {
    'FREE_FLOW': 'LOW',
    'LIGHT': 'MODERATE',
    'MODERATE': 'HIGH',
    'HEAVY': 'SEVERE',
    'SEVERE': 'SEVERE',
  }
  return mapping[level] || level
}

// Sensor locations for Prishtina area (for sensors without coordinates)
function getSensorLocation(sensorCode: string): { lat: number; lng: number } {
  const locations: Record<string, { lat: number; lng: number }> = {
    'SENSOR-001': { lat: 42.6629, lng: 21.1655 },
    'SENSOR-002': { lat: 42.6700, lng: 21.1500 },
    'SENSOR-003': { lat: 42.6550, lng: 21.1800 },
    'SENSOR-004': { lat: 42.6580, lng: 21.1400 },
    'SENSOR-005': { lat: 42.6750, lng: 21.1700 },
    'SENSOR-006': { lat: 42.6500, lng: 21.1550 },
    'SENSOR-007': { lat: 42.6650, lng: 21.1900 },
    'SENSOR-008': { lat: 42.6720, lng: 21.1600 },
  }
  return locations[sensorCode] || { lat: 42.6629, lng: 21.1655 }
}

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


export async function getRoadOverlays(): Promise<any[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/traffic/roads`)
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

