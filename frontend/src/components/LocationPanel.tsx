interface LocationPanelProps {
  location: string
}

function LocationPanel({ location }: LocationPanelProps) {
  return (
    <div className="bg-white rounded-lg shadow-lg p-4 min-w-[200px]">
      <div className="text-gray-600 text-sm font-medium mb-2">Location</div>
      <div className="text-2xl font-bold text-blue-900">{location}</div>
    </div>
  )
}

export default LocationPanel

