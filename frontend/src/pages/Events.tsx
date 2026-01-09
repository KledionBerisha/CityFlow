import { useEffect, useState } from 'react'
import * as api from '../services/api'

function Events() {
  const [incidents, setIncidents] = useState<any[]>([])

  useEffect(() => {
    const fetchIncidents = async () => {
      const data = await api.getIncidents()
      setIncidents(data)
    }
    fetchIncidents()
  }, [])

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold text-gray-800 mb-6">Events</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Recent Incidents</h2>
        {incidents.length === 0 ? (
          <p className="text-gray-600">No incidents found.</p>
        ) : (
          <ul className="space-y-2">
            {incidents.map((incident) => (
              <li key={incident.id} className="border-b pb-2">
                <div className="font-medium">{incident.type || 'Unknown'}</div>
                <div className="text-sm text-gray-500">
                  {incident.timestamp || 'No timestamp'}
                </div>
              </li>
            ))}
          </ul>
        )}
        <p className="text-sm text-gray-500 mt-4">
          TODO: Connect to GET /api/incidents endpoint
        </p>
      </div>
    </div>
  )
}

export default Events

