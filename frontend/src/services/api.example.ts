/**
 * Example: How to connect actual backend endpoints
 * 
 * This file shows examples of how to replace placeholder implementations
 * with actual API calls. Copy the patterns to src/services/api.ts
 */

// ============================================================================
// Example 1: Simple GET request with error handling
// ============================================================================

export async function getVehicleCountExample() {
  try {
    const response = await fetch('/api/traffic/vehicle-count', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        // Add auth header when ready:
        // 'Authorization': `Bearer ${getAuthToken()}`
      },
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    return data // { avg: number, max: number, min: number }
  } catch (error) {
    console.error('Error fetching vehicle count:', error)
    // Return fallback data or re-throw
    throw error
  }
}

// ============================================================================
// Example 2: GET request with query parameters
// ============================================================================

export async function getAccidentCountExample(hours: number = 24) {
  const url = new URL('/api/incidents/count', window.location.origin)
  url.searchParams.append('hours', hours.toString())
  
  const response = await fetch(url.toString())
  if (!response.ok) throw new Error('Failed to fetch')
  
  return await response.json() // { count: number }
}

// ============================================================================
// Example 3: Handling GeoJSON data
// ============================================================================

export async function getRoadOverlaysExample() {
  const response = await fetch('/api/map/roads')
  if (!response.ok) throw new Error('Failed to fetch roads')
  
  const data = await response.json()
  
  // Handle both FeatureCollection and array of features
  if (data.type === 'FeatureCollection') {
    return data.features
  } else if (Array.isArray(data)) {
    return data
  } else {
    return []
  }
}

// ============================================================================
// Example 4: Using Server-Sent Events (SSE) for real-time updates
// ============================================================================

export function subscribeToBusLocations(callback: (data: any) => void) {
  const eventSource = new EventSource('/api/bus-locations/stream')
  
  eventSource.addEventListener('location-update', (event) => {
    const data = JSON.parse(event.data)
    callback(data)
  })
  
  eventSource.onerror = (error) => {
    console.error('SSE error:', error)
    eventSource.close()
  }
  
  // Return cleanup function
  return () => eventSource.close()
}

// Usage in component:
// useEffect(() => {
//   const cleanup = subscribeToBusLocations((data) => {
//     setBusLocations(data)
//   })
//   return cleanup
// }, [])

// ============================================================================
// Example 5: POST request with body
// ============================================================================

export async function createRouteExample(routeData: {
  routeCode: string
  routeName: string
  isActive: boolean
}) {
  const response = await fetch('/api/routes', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(routeData),
  })
  
  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to create route')
  }
  
  return await response.json()
}

