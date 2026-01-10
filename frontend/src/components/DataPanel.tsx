interface DataPanelProps {
  title: string
  value: string
  subtitle: string | null
  stats: { max: number; min: number } | null
}

function DataPanel({ title, value, subtitle, stats }: DataPanelProps) {
  return (
    <div className="bg-white rounded-lg shadow-lg p-4 min-w-[200px]">
      <div className="text-gray-600 text-sm font-medium mb-2">{title}</div>
      <div className="flex items-baseline gap-2">
        <span className="text-2xl font-bold text-blue-900">{value}</span>
        {subtitle && (
          <span className="text-sm font-medium text-gray-600">{subtitle}</span>
        )}
      </div>
      {stats && (
        <div className="text-xs text-gray-500 mt-2">
          MAX - {stats.max} | MIN - {stats.min}
        </div>
      )}
    </div>
  )
}

export default DataPanel

