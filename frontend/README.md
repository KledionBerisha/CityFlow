# CityFlow Frontend

React-based frontend application for the CityFlow real-time traffic monitoring system.

## Features

- **Live Map**: Real-time visualization of city traffic with interactive map
- **Dashboard**: Overview of traffic statistics and metrics
- **Events**: Display of traffic incidents and events
- **Predict**: Traffic prediction and forecasting
- **Settings**: Application configuration

## Tech Stack

- **React 18** with TypeScript
- **Vite** for build tooling
- **React Router** for navigation
- **Leaflet** for map visualization
- **Tailwind CSS** for styling
- **Lucide React** for icons

## Getting Started

### Prerequisites

- Node.js 18+ and npm/yarn/pnpm

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Build

```bash
npm run build
```

## Backend Integration

The frontend is configured to connect to the backend API Gateway at `http://localhost:8000/api`.

### API Service Layer

All API calls are centralized in `src/services/api.ts`. This file contains:

- Placeholder implementations with fallback data for development
- Clear TODO comments indicating which backend endpoints to connect
- TypeScript interfaces for all data structures

### Key API Endpoints to Connect

1. **Vehicle Count**: `GET /api/traffic/vehicle-count`
2. **Accident Count**: `GET /api/incidents/count?hours=24`
3. **Current Location**: `GET /api/location/current`
4. **Bus Locations**: `GET /api/bus-locations/current` (already available)
5. **Routes**: `GET /api/routes` (already available)
6. **Traffic Readings**: `GET /api/traffic/readings/current`
7. **Predictions**: `GET /api/analytics/predictions`

### Proxy Configuration

The Vite dev server is configured to proxy `/api` requests to `http://localhost:8000` (see `vite.config.ts`).

## Project Structure

```
frontend/
├── src/
│   ├── components/      # Reusable UI components
│   │   ├── Layout.tsx
│   │   ├── Sidebar.tsx
│   │   ├── DataPanel.tsx
│   │   └── LocationPanel.tsx
│   ├── pages/           # Page components
│   │   ├── Dashboard.tsx
│   │   ├── LiveMap.tsx
│   │   ├── Events.tsx
│   │   ├── Predict.tsx
│   │   └── Settings.tsx
│   ├── services/        # API service layer
│   │   └── api.ts
│   ├── hooks/           # Custom React hooks
│   │   └── useMapData.ts
│   ├── App.tsx          # Main app component
│   ├── main.tsx         # Entry point
│   └── index.css        # Global styles
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

## Design Notes

- **Color Scheme**: Blue (#2563eb) for primary branding
- **Map**: Light beige background with yellow-highlighted roads
- **Data Panels**: White cards with blue text for metrics
- **Sidebar**: White background with blue active state

## Next Steps

1. Connect API endpoints in `src/services/api.ts`
2. Add road overlay visualization to the map
3. Implement real-time updates using WebSockets or SSE
4. Add authentication/authorization
5. Enhance map with bus markers and traffic overlays

