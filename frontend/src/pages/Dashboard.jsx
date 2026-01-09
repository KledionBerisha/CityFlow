import MapView from "../components/MapView";
import TopStats from "../components/TopStats";

export default function Dashboard() {
  return (
    <div className="relative w-full h-screen">
      <MapView />

      <div className="absolute top-6 right-6 z-[1000]">
        <TopStats />
      </div>
    </div>
  );
}


