import { useEffect, useState } from 'react'
import * as api from '../services/api'

interface Incident {
  id: string
  incidentCode: string
  type: string
  severity: string
  status: string
  title: string
  description: string
  latitude: number
  longitude: number
  roadSegmentId?: string
  sourceId?: string
  sourceType?: string
  detectedAt: string
  confidence?: number
}

function Events() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchIncidents = async () => {
      try {
        // Use recent incidents (last 24 hours) to match the live map count
        const data = await api.getRecentIncidents(24)
        setIncidents(data)
      } catch (error) {
        console.error('Error fetching incidents:', error)
        setIncidents([])
      } finally {
        setLoading(false)
      }
    }
    fetchIncidents()
    
    // Refresh every 30 seconds
    const interval = setInterval(fetchIncidents, 30000)
    return () => clearInterval(interval)
  }, [])

  const getSeverityColor = (severity: string) => {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return 'bg-red-100 text-red-800 border-red-300'
      case 'HIGH': return 'bg-orange-100 text-orange-800 border-orange-300'
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 border-yellow-300'
      case 'LOW': return 'bg-green-100 text-green-800 border-green-300'
      default: return 'bg-gray-100 text-gray-800 border-gray-300'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'DETECTED': return '🔴'
      case 'CONFIRMED': return '🟠'
      case 'IN_PROGRESS': return '🟡'
      case 'RESOLVED': return '✅'
      case 'DISMISSED': return '❌'
      default: return '⚪'
    }
  }

  return (
    <div className="p-8">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">Events & Incidents</h1>
        
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold">Recent Incidents</h2>
            <span className="text-sm text-gray-500">
              Auto-refreshes every 30 seconds
            </span>
          </div>
          
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            </div>
          ) : incidents.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-5xl mb-4">✅</div>
              <p className="text-gray-600 font-medium">No incidents detected</p>
              <p className="text-sm text-gray-500 mt-2">
                All traffic conditions are normal. The system will automatically detect and display incidents when they occur.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {incidents.map((incident) => (
                <div
                  key={incident.id}
                  className={`border rounded-lg p-4 ${getSeverityColor(incident.severity)}`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="flex items-center gap-2">
                        <span>{getStatusIcon(incident.status)}</span>
                        <span className="font-semibold">{incident.title || incident.type}</span>
                        <span className="text-xs px-2 py-0.5 rounded bg-white/50">
                          {incident.severity}
                        </span>
                      </div>
                      <p className="text-sm mt-1">{incident.description}</p>
                      <div className="text-xs mt-2 opacity-75">
                        <span>📍 {incident.roadSegmentId || 'Unknown location'}</span>
                        <span className="mx-2">•</span>
                        <span>🕐 {new Date(incident.detectedAt).toLocaleString()}</span>
                        {incident.confidence && (
                          <>
                            <span className="mx-2">•</span>
                            <span>Confidence: {(incident.confidence * 100).toFixed(0)}%</span>
                          </>
                        )}
                      </div>
                    </div>
                    <span className="text-xs font-mono">{incident.incidentCode}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
    </div>
  )
}

export default Events

