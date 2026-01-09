import MapView from "../components/MapView";

export default function LiveMap() {
	return (
		<div className="h-full min-h-screen p-4 lg:p-6 bg-gray-50">
			<div className="h-full">
				<MapView />
			</div>
		</div>
	);
}
