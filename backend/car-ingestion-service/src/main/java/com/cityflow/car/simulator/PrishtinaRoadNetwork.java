package com.cityflow.car.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Defines the road network of Prishtina for realistic traffic simulation.
 * Roads are defined as sequences of coordinate waypoints that cars follow.
 */
public class PrishtinaRoadNetwork {

    /**
     * Major roads in Prishtina with their waypoints.
     * Each road has: name, speedLimit (km/h), lane count, and list of [lat, lon] waypoints
     */
    public static final List<Road> ROADS = createRoadNetwork();

    private static List<Road> createRoadNetwork() {
        List<Road> roads = new ArrayList<>();

        // Bill Clinton Boulevard - Main east-west arterial through city center
        roads.add(new Road("BILL-CLINTON-BLVD", "Bill Clinton Boulevard", 50, 4, true, List.of(
                new double[]{42.6625, 21.1420},  // West end (near Sunny Hill)
                new double[]{42.6620, 21.1500},
                new double[]{42.6615, 21.1570},
                new double[]{42.6610, 21.1610},  // Government area
                new double[]{42.6620, 21.1650},  // City center
                new double[]{42.6625, 21.1700},
                new double[]{42.6628, 21.1750}   // East connection
        )));

        // Mother Teresa Boulevard - North-south through center
        roads.add(new Road("MOTHER-TERESA-BLVD", "Mother Teresa Boulevard", 50, 4, true, List.of(
                new double[]{42.6700, 21.1640},  // North end
                new double[]{42.6660, 21.1638},
                new double[]{42.6625, 21.1635},  // Bill Clinton intersection
                new double[]{42.6600, 21.1630},  // Grand Hotel
                new double[]{42.6570, 21.1625},  // University area
                new double[]{42.6520, 21.1620}   // South end
        )));

        // Agim Ramadani Street - Important connector
        roads.add(new Road("AGIM-RAMADANI", "Agim Ramadani Street", 40, 2, true, List.of(
                new double[]{42.6630, 21.1660},  // North
                new double[]{42.6600, 21.1645},
                new double[]{42.6570, 21.1630},
                new double[]{42.6540, 21.1615},
                new double[]{42.6500, 21.1600}   // South - Arbëria direction
        )));

        // Ring Road M9 - High speed bypass
        roads.add(new Road("M9-RING-ROAD", "Ring Road M9", 80, 4, true, List.of(
                new double[]{42.6850, 21.1500},  // Northwest
                new double[]{42.6800, 21.1600},
                new double[]{42.6780, 21.1700},
                new double[]{42.6770, 21.1800},
                new double[]{42.6760, 21.1900}   // Northeast
        )));

        // Luan Haradinaj Street
        roads.add(new Road("LUAN-HARADINAJ", "Luan Haradinaj Street", 40, 2, false, List.of(
                new double[]{42.6650, 21.1580},
                new double[]{42.6620, 21.1560},
                new double[]{42.6590, 21.1540},
                new double[]{42.6560, 21.1520}
        )));

        // Fehmi Agani Street
        roads.add(new Road("FEHMI-AGANI", "Fehmi Agani Street", 40, 2, false, List.of(
                new double[]{42.6640, 21.1700},
                new double[]{42.6610, 21.1680},
                new double[]{42.6580, 21.1660},
                new double[]{42.6550, 21.1640}
        )));

        // Dardania neighborhood roads
        roads.add(new Road("DARDANIA-MAIN", "Dardania Main Road", 40, 2, false, List.of(
                new double[]{42.6580, 21.1500},
                new double[]{42.6550, 21.1520},
                new double[]{42.6520, 21.1540},
                new double[]{42.6490, 21.1560}
        )));

        // Ulpiana neighborhood
        roads.add(new Road("ULPIANA-MAIN", "Ulpiana Main Street", 40, 2, false, List.of(
                new double[]{42.6600, 21.1580},
                new double[]{42.6570, 21.1560},
                new double[]{42.6540, 21.1540},
                new double[]{42.6510, 21.1520}
        )));

        // Arbëria road
        roads.add(new Road("ARBERIA-ROAD", "Arbëria Road", 50, 2, true, List.of(
                new double[]{42.6550, 21.1600},
                new double[]{42.6510, 21.1570},
                new double[]{42.6470, 21.1540},
                new double[]{42.6430, 21.1510},
                new double[]{42.6390, 21.1480}
        )));

        // Dragodan neighborhood
        roads.add(new Road("DRAGODAN-MAIN", "Dragodan Main Road", 40, 2, false, List.of(
                new double[]{42.6550, 21.1700},
                new double[]{42.6520, 21.1730},
                new double[]{42.6490, 21.1760},
                new double[]{42.6460, 21.1790}
        )));

        // Germia access road
        roads.add(new Road("GERMIA-ROAD", "Germia Park Road", 60, 2, false, List.of(
                new double[]{42.6650, 21.1750},
                new double[]{42.6680, 21.1800},
                new double[]{42.6710, 21.1860},
                new double[]{42.6740, 21.1920}
        )));

        // Mati neighborhood road
        roads.add(new Road("MATI-ROAD", "Mati Road", 40, 2, false, List.of(
                new double[]{42.6600, 21.1750},
                new double[]{42.6580, 21.1800},
                new double[]{42.6560, 21.1850},
                new double[]{42.6540, 21.1900}
        )));

        // Veternik road
        roads.add(new Road("VETERNIK-ROAD", "Veternik Road", 50, 2, false, List.of(
                new double[]{42.6700, 21.1550},
                new double[]{42.6730, 21.1560},
                new double[]{42.6760, 21.1570},
                new double[]{42.6800, 21.1580}
        )));

        // Sunny Hill area
        roads.add(new Road("SUNNY-HILL-MAIN", "Sunny Hill Main Street", 40, 2, false, List.of(
                new double[]{42.6660, 21.1420},
                new double[]{42.6640, 21.1440},
                new double[]{42.6620, 21.1460},
                new double[]{42.6600, 21.1480}
        )));

        // Industrial zone road
        roads.add(new Road("INDUSTRIAL-ROAD", "Industrial Zone Road", 50, 2, false, List.of(
                new double[]{42.6450, 21.1600},
                new double[]{42.6430, 21.1650},
                new double[]{42.6410, 21.1700},
                new double[]{42.6390, 21.1750}
        )));

        return roads;
    }

    /**
     * Get a random road for a new car to start on
     */
    public static Road getRandomRoad() {
        return ROADS.get(ThreadLocalRandom.current().nextInt(ROADS.size()));
    }

    /**
     * Get roads near a given location (for cars to switch roads at intersections)
     */
    public static List<Road> getRoadsNearLocation(double lat, double lon, double radiusKm) {
        List<Road> nearbyRoads = new ArrayList<>();
        for (Road road : ROADS) {
            for (double[] waypoint : road.waypoints) {
                if (calculateDistance(lat, lon, waypoint[0], waypoint[1]) <= radiusKm) {
                    nearbyRoads.add(road);
                    break;
                }
            }
        }
        return nearbyRoads;
    }

    /**
     * Get congestion hotspots - areas where traffic typically builds up
     */
    public static List<CongestionHotspot> getCongestionHotspots() {
        return List.of(
                // City center - highest congestion during peak hours
                new CongestionHotspot("CITY-CENTER", 42.6610, 21.1630, 0.3, 0.9, List.of(7, 8, 9, 17, 18, 19)),
                
                // Bill Clinton & Mother Teresa intersection
                new CongestionHotspot("BILL-MOTHER-INTERSECTION", 42.6625, 21.1640, 0.2, 0.8, List.of(8, 9, 17, 18)),
                
                // University area - busy during class hours
                new CongestionHotspot("UNIVERSITY-AREA", 42.6570, 21.1635, 0.25, 0.7, List.of(8, 9, 12, 13, 16, 17)),
                
                // Bus station area
                new CongestionHotspot("BUS-STATION", 42.6632, 21.1655, 0.2, 0.75, List.of(7, 8, 17, 18, 19)),
                
                // Government area
                new CongestionHotspot("GOVT-AREA", 42.6608, 21.1570, 0.2, 0.65, List.of(8, 9, 16, 17)),
                
                // Ring road entry points
                new CongestionHotspot("M9-ENTRY-NORTH", 42.6800, 21.1600, 0.3, 0.6, List.of(7, 8, 17, 18))
        );
    }

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Road definition
     */
    public record Road(
            String id,
            String name,
            int speedLimitKmh,
            int laneCount,
            boolean isMajorRoad,
            List<double[]> waypoints
    ) {
        public double[] getRandomStartPoint() {
            int index = ThreadLocalRandom.current().nextInt(waypoints.size());
            return waypoints.get(index);
        }

        public int getDirection() {
            // 0 = forward (start to end), 1 = backward (end to start)
            return ThreadLocalRandom.current().nextInt(2);
        }
    }

    /**
     * Congestion hotspot - areas with high traffic density
     */
    public record CongestionHotspot(
            String id,
            double centerLat,
            double centerLon,
            double radiusKm,
            double congestionFactor,  // 0.0 to 1.0 - how much to slow traffic
            List<Integer> peakHours   // Hours when congestion is highest
    ) {
        public boolean isInHotspot(double lat, double lon) {
            return calculateDistance(centerLat, centerLon, lat, lon) <= radiusKm;
        }

        public double getCongestionMultiplier(int currentHour) {
            if (peakHours.contains(currentHour)) {
                return congestionFactor;
            }
            return congestionFactor * 0.3; // Reduced congestion outside peak hours
        }
    }
}
