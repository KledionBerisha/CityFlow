package com.cityflow.bus.simulator;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Real Prishtina Bus Routes based on Trafiku Urban data.
 * 
 * Source: Trafiku Urban app (developed by Appbites for Municipality of Prishtina)
 * Lines covered: 1, 1A, 3, 4
 * 
 * Note: As there is no public API, these routes are based on publicly available
 * information about Prishtina's urban transport system.
 */
public class PrishtinaBusRoutes {

    /**
     * Line 1: Stacioni i Autobusëve - Veternik
     * Main north-south route through city center to Veternik residential area.
     * Frequency: Every 15 minutes (primary line)
     * Operating hours: 05:30 - 23:00
     */
    public static final BusRoute LINE_1 = new BusRoute(
        "LINE-1",
        "Linja 1: Qendra - Veternik",
        "1",
        15, // frequency in minutes
        LocalTime.of(5, 30),
        LocalTime.of(23, 0),
        List.of(
            new BusStop("Stacioni i Autobusëve", 42.6632, 21.1655, true, 0),
            new BusStop("Bulevardi Bill Clinton", 42.6625, 21.1658, false, 2),
            new BusStop("Sheshi Nëna Terezë", 42.6600, 21.1637, false, 4),
            new BusStop("Monumenti Newborn", 42.6594, 21.1610, false, 6),
            new BusStop("Qeveria", 42.6608, 21.1570, false, 8),
            new BusStop("Rrethi i Lidhjes", 42.6630, 21.1520, false, 10),
            new BusStop("Kodra e Diellit", 42.6650, 21.1450, false, 13),
            new BusStop("Rr. Veternik", 42.6720, 21.1480, false, 16),
            new BusStop("Veternik Qendër", 42.6800, 21.1500, true, 20)
        )
    );

    /**
     * Line 1A: Stacioni i Autobusëve - Aeroporti Ndërkombëtar i Prishtinës
     * Airport express line (rapid service).
     * Frequency: Every 60 minutes
     * Operating hours: 05:00 - 22:00
     */
    public static final BusRoute LINE_1A = new BusRoute(
        "LINE-1A",
        "Linja 1A: Qendra - Aeroport",
        "1A",
        60, // frequency in minutes
        LocalTime.of(5, 0),
        LocalTime.of(22, 0),
        List.of(
            new BusStop("Stacioni i Autobusëve", 42.6632, 21.1655, true, 0),
            new BusStop("Bulevardi Bill Clinton", 42.6625, 21.1658, false, 2),
            new BusStop("Grand Hotel Prishtina", 42.6597, 21.1627, false, 4),
            new BusStop("Rruga Agim Ramadani", 42.6580, 21.1632, false, 6),
            new BusStop("Fushe Kosove Hyrja", 42.6350, 21.1200, false, 15),
            new BusStop("Aeroporti Adem Jashari", 42.5728, 21.0358, true, 30)
        )
    );

    /**
     * Line 3: Stacioni i Autobusëve - Mati/Germia
     * Eastern route to Mati neighborhood and Germia Park.
     * Frequency: Every 20 minutes
     * Operating hours: 05:30 - 22:30
     */
    public static final BusRoute LINE_3 = new BusRoute(
        "LINE-3",
        "Linja 3: Qendra - Mati - Germia",
        "3",
        20, // frequency in minutes
        LocalTime.of(5, 30),
        LocalTime.of(22, 30),
        List.of(
            new BusStop("Stacioni i Autobusëve", 42.6632, 21.1655, true, 0),
            new BusStop("Grand Hotel Prishtina", 42.6597, 21.1627, false, 3),
            new BusStop("Universiteti i Prishtinës", 42.6570, 21.1645, false, 5),
            new BusStop("Lagjja Dardania", 42.6540, 21.1700, false, 8),
            new BusStop("Dragodan", 42.6520, 21.1750, false, 11),
            new BusStop("Mati 1", 42.6560, 21.1850, false, 14),
            new BusStop("Mati 2", 42.6600, 21.1900, false, 17),
            new BusStop("Parku Germia Hyrja", 42.6680, 21.1950, false, 20),
            new BusStop("Germia Qendër", 42.6750, 21.2000, true, 25)
        )
    );

    /**
     * Line 4: Stacioni i Autobusëve - Arbëria
     * Southern route to Arbëria neighborhood.
     * Frequency: Every 20 minutes
     * Operating hours: 06:00 - 22:00
     */
    public static final BusRoute LINE_4 = new BusRoute(
        "LINE-4",
        "Linja 4: Qendra - Arbëria",
        "4",
        20, // frequency in minutes
        LocalTime.of(6, 0),
        LocalTime.of(22, 0),
        List.of(
            new BusStop("Stacioni i Autobusëve", 42.6632, 21.1655, true, 0),
            new BusStop("Sheshi Skënderbeu", 42.6601, 21.1637, false, 2),
            new BusStop("Rruga Agim Ramadani", 42.6580, 21.1632, false, 4),
            new BusStop("Ulpiana", 42.6550, 21.1580, false, 7),
            new BusStop("Lagjja e Re", 42.6510, 21.1560, false, 10),
            new BusStop("Arbëria 1", 42.6470, 21.1540, false, 13),
            new BusStop("Arbëria 2", 42.6430, 21.1520, false, 16),
            new BusStop("Arbëria Qendër", 42.6390, 21.1500, true, 20)
        )
    );

    /**
     * All available routes
     */
    public static final List<BusRoute> ALL_ROUTES = List.of(LINE_1, LINE_1A, LINE_3, LINE_4);

    /**
     * Get route by line number
     */
    public static BusRoute getRouteByLine(String lineNumber) {
        return ALL_ROUTES.stream()
            .filter(r -> r.lineNumber().equals(lineNumber))
            .findFirst()
            .orElse(null);
    }

    /**
     * Bus route definition
     */
    public record BusRoute(
        String id,
        String name,
        String lineNumber,
        int frequencyMinutes,
        LocalTime operatingStart,
        LocalTime operatingEnd,
        List<BusStop> stops
    ) {
        /**
         * Check if this route is currently operating
         */
        public boolean isOperating(LocalTime currentTime) {
            return !currentTime.isBefore(operatingStart) && !currentTime.isAfter(operatingEnd);
        }

        /**
         * Get total route duration in minutes
         */
        public int getTotalDurationMinutes() {
            return stops.get(stops.size() - 1).minutesFromStart();
        }

        /**
         * Get distance between two stops in km (approximate)
         */
        public double getDistanceBetweenStops(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex >= stops.size() || fromIndex >= toIndex) {
                return 0;
            }
            BusStop from = stops.get(fromIndex);
            BusStop to = stops.get(toIndex);
            return calculateDistance(from.latitude(), from.longitude(), to.latitude(), to.longitude());
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371; // Earth radius in km
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }

    /**
     * Bus stop definition
     */
    public record BusStop(
        String name,
        double latitude,
        double longitude,
        boolean isTerminal,
        int minutesFromStart // estimated travel time from route start
    ) {}

    /**
     * Bus vehicle assignment - simulates 4 buses, one per line
     */
    public static final Map<String, BusVehicle> VEHICLES = Map.of(
        "BUS-001", new BusVehicle("BUS-001", "01-234-KS", "Mercedes Citaro", "LINE-1", 50),
        "BUS-002", new BusVehicle("BUS-002", "01-567-KS", "MAN Lion's City", "LINE-1A", 45),
        "BUS-003", new BusVehicle("BUS-003", "01-890-KS", "Mercedes Citaro", "LINE-3", 50),
        "BUS-004", new BusVehicle("BUS-004", "01-123-KS", "MAN Lion's City", "LINE-4", 45)
    );

    /**
     * Bus vehicle definition
     */
    public record BusVehicle(
        String vehicleId,
        String licensePlate,
        String model,
        String assignedRoute,
        int passengerCapacity
    ) {}
}
