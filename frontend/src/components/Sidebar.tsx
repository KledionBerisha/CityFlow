import { Link, useLocation } from 'react-router-dom'
import { Gauge, Map, Clock, ArrowRight, Settings } from 'lucide-react'

const menuItems = [
  { path: '/dashboard', icon: Gauge, label: 'Dashboard' },
  { path: '/live-map', icon: Map, label: 'Live Map' },
  { path: '/events', icon: Clock, label: 'Events' },
  { path: '/predict', icon: ArrowRight, label: 'Predict' },
  { path: '/settings', icon: Settings, label: 'Settings' },
]

function Sidebar() {
  const location = useLocation()

  return (
    <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
      {/* Logo */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-cityflow-primary rounded flex items-center justify-center">
            <svg
              width="20"
              height="20"
              viewBox="0 0 20 20"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <rect x="4" y="4" width="3" height="3" fill="white" />
              <rect x="9" y="4" width="3" height="3" fill="white" />
              <rect x="13" y="4" width="3" height="3" fill="white" />
              <rect x="4" y="9" width="3" height="3" fill="white" />
              <rect x="9" y="9" width="3" height="3" fill="white" />
              <rect x="13" y="9" width="3" height="3" fill="white" />
              <rect x="4" y="13" width="3" height="3" fill="white" />
              <rect x="9" y="13" width="3" height="3" fill="white" />
              <rect x="13" y="13" width="3" height="3" fill="white" />
            </svg>
          </div>
          <div className="flex flex-col">
            <span className="text-cityflow-primary font-bold text-sm leading-tight">CITY</span>
            <span className="text-cityflow-primary font-bold text-sm leading-tight">FLOW</span>
          </div>
        </div>
      </div>

      {/* Navigation Menu */}
      <nav className="flex-1 p-4">
        <ul className="space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.path
            return (
              <li key={item.path}>
                <Link
                  to={item.path}
                  className={`
                    flex items-center gap-3 px-4 py-3 rounded-lg transition-colors
                    ${
                      isActive
                        ? 'bg-cityflow-primary text-white'
                        : 'text-gray-700 hover:bg-gray-100'
                    }
                  `}
                >
                  <Icon size={20} />
                  <span className="font-medium">{item.label}</span>
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>
    </aside>
  )
}

export default Sidebar

