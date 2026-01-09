import { useEffect, useState } from "react";

export default function TopStats() {
  const [time, setTime] = useState(new Date());

  useEffect(() => {
    const interval = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  const formattedTime = time.toLocaleTimeString();

  return (
    <div className="flex flex-col gap-4">
      <StatCard title="Current Time">
        <span className="text-3xl font-bold text-gray-900 tabular-nums">
          {formattedTime}
        </span>
      </StatCard>

      <StatCard title="Vehicle Count">
        <div className="flex items-baseline gap-2">
          <span className="text-3xl font-bold text-gray-900">173</span>
          <span className="text-sm font-medium text-gray-500">AVG</span>
        </div>
        <div className="flex gap-3 mt-2 text-xs font-medium">
          <span className="text-red-600">MAX - 371</span>
          <span className="text-green-600">MIN - 87</span>
        </div>
      </StatCard>

      <StatCard title="Accidents">
        <span className="text-3xl font-bold text-gray-900">
          2 <span className="text-lg text-gray-500">/ 24Hrs</span>
        </span>
      </StatCard>

      <StatCard title="Location">
        <span className="text-2xl font-semibold text-indigo-600">
          Prishtina
        </span>
      </StatCard>
    </div>
  );
}

function StatCard({ title, children }) {
  return (
    <div className="bg-white/100 backdrop-blur-sm rounded-lg shadow-xl p-5 w-64 border-2 border-gray-300" style={{ backgroundColor: "#ffffff" }}>
      <p className="text-xs font-medium text-gray-600 mb-3 uppercase tracking-wide">{title}</p>
      {children}
    </div>
  );
}