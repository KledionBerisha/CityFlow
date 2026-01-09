import Sidebar from "../components/Sidebar";
import { Outlet } from "react-router-dom";

export default function DashboardLayout() {
  return (
    <div className="flex h-screen w-screen overflow-hidden">
      <Sidebar />
      <main className="flex-1 ml-[240px] bg-gray-50 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
