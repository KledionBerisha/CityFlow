import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";

const buses = [
  { id: "R12-103", route: "R12", status: "delayed", delay: "5m", eta: "3m", position: [42.665, 21.17] },
  { id: "R5-044", route: "R5", status: "ontime", delay: "0m", eta: "2m", position: [42.661, 21.163] },
  { id: "R28-021", route: "R28", status: "minor", delay: "2m", eta: "6m", position: [42.668, 21.158] },
  { id: "R15-077", route: "R15", status: "ontime", delay: "0m", eta: "4m", position: [42.657, 21.175] },
];

const statusColor = {
  ontime: "#16a34a",
  minor: "#f59e0b",
  delayed: "#ef4444",
};

export default function MapView() {
  const center = [42.6629, 21.1655];

  return (
    <div className="absolute inset-0 w-full h-full">
      <MapContainer
        center={center}
        zoom={13}
        className="h-full w-full"
        scrollWheelZoom
      >
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {buses.map((bus) => (
          <CircleMarker
            key={bus.id}
            center={bus.position}
            radius={12}
            pathOptions={{
              color: statusColor[bus.status],
              fillColor: statusColor[bus.status],
              fillOpacity: 0.75,
            }}
          >
            <Popup>
              <div className="space-y-1">
                <p className="font-semibold">Route {bus.route}</p>
                <p className="text-sm">Bus ID: {bus.id}</p>
                <p className="text-sm">ETA: {bus.eta}</p>
                <p className="text-sm">Delay: {bus.delay}</p>
              </div>
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  );
}
