import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Map,
  AlertTriangle,
  TrendingUp,
  Settings,
  Navigation,
} from "lucide-react";

const mainMenu = [
  { name: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { name: "Live Map", path: "/live-map", icon: Map },
  { name: "Events", path: "/events", icon: AlertTriangle },
  { name: "Predict", path: "/predict", icon: TrendingUp },
];

export default function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 h-screen w-[240px] bg-white px-6 py-6 flex flex-col z-50 overflow-hidden">
      
      {/* Logo */}
      <div className="flex items-center gap-3 mb-10">
        <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold">
          
        </div>
        <span className="text-xl font-semibold tracking-wide">
          CITY FLOW
        </span>
      </div>

      {/* Navigation */}
      <nav className="flex flex-col gap-5 flex-1">
        {mainMenu.map(({ name, path, icon: Icon }) => (
          <NavLink
            key={name}
            to={path}
            className={({ isActive }) =>
              `flex items-center gap-3 h-10 px-3 rounded-lg cursor-pointer transition ${
                isActive
                  ? "bg-indigo-50 text-indigo-600"
                  : "text-gray-700 hover:bg-indigo-50 hover:text-indigo-600"
              }`
            }
          >
            <Icon size={20} />
            <span className="text-sm font-medium">{name}</span>
          </NavLink>
        ))}
      </nav>

      {/* Settings */}
    <div className="pb-2">
        <NavLink
        to="/settings"
        className={({ isActive }) =>
            `flex items-center gap-3 h-10 px-3 rounded-lg cursor-pointer transition ${
            isActive
                ? "bg-indigo-50 text-indigo-600"
                : "text-gray-700 hover:bg-indigo-50 hover:text-indigo-600"
            }`
        }
        >
        <Settings size={20} />
        <span className="text-sm font-medium">Settings</span>
        </NavLink>
    </div>
    </aside>
  );
}