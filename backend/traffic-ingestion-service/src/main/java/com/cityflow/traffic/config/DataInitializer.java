package com.cityflow.traffic.config;

import com.cityflow.traffic.model.Sensor;
import com.cityflow.traffic.model.SensorStatus;
import com.cityflow.traffic.model.SensorType;
import com.cityflow.traffic.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SensorRepository sensorRepository;

    public DataInitializer(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    @Override
    public void run(String... args) {
        sensorRepository.count()
                .flatMapMany(count -> {
                    if (count > 0) {
                        log.info("Database already contains {} sensors, skipping initialization", count);
                        return Flux.empty();
                    }

                    log.info("Initializing sample sensors...");
                    return Flux.fromIterable(createSampleSensors())
                            .flatMap(sensorRepository::save);
                })
                .collectList()
                .subscribe(
                        sensors -> {
                            if (!sensors.isEmpty()) {
                                log.info("Created {} sample sensors", sensors.size());
                                sensors.forEach(s -> log.info("  - {} ({}) at [{}, {}]", 
                                        s.getName(), s.getCode(), 
                                        s.getLocation().getLatitude(), 
                                        s.getLocation().getLongitude()));
                            }
                        },
                        error -> log.error("Failed to initialize sensors", error)
                );
    }

    private List<Sensor> createSampleSensors() {
        // Real Prishtina, Kosovo intersection coordinates
        // Multiple sensors per road segment for better ML predictions and valid LineString geometry
        return Arrays.asList(
                // Bill Clinton Boulevard - 3 sensors along the boulevard
                createSensor("SENSOR-001", "Bill Clinton Blvd & Mother Teresa Square", 42.6625, 21.1658, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-BILL-CLINTON-01", 4, 50.0),
                createSensor("SENSOR-016", "Bill Clinton Blvd Mid-Section", 42.6615, 21.1650, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-BILL-CLINTON-01", 4, 50.0),
                createSensor("SENSOR-017", "Bill Clinton Blvd South End", 42.6605, 21.1642, 
                        SensorType.LOOP_DETECTOR, SensorStatus.ACTIVE, "ROAD-BILL-CLINTON-01", 4, 50.0),
                
                // Agim Ramadani Street - 3 sensors along north-south arterial
                createSensor("SENSOR-002", "Agim Ramadani Street North", 42.6590, 21.1632, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-AGIM-RAMADANI-01", 4, 50.0),
                createSensor("SENSOR-018", "Agim Ramadani Street Central", 42.6580, 21.1632, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-AGIM-RAMADANI-01", 4, 50.0),
                createSensor("SENSOR-019", "Agim Ramadani Street South", 42.6570, 21.1632, 
                        SensorType.LOOP_DETECTOR, SensorStatus.ACTIVE, "ROAD-AGIM-RAMADANI-01", 4, 50.0),
                
                // Grand Hotel Roundabout - 2 sensors
                createSensor("SENSOR-003", "Grand Hotel Roundabout East", 42.6597, 21.1627, 
                        SensorType.LOOP_DETECTOR, SensorStatus.ACTIVE, "ROAD-GRAND-HOTEL-01", 6, 40.0),
                createSensor("SENSOR-020", "Grand Hotel Roundabout West", 42.6597, 21.1615, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-GRAND-HOTEL-01", 6, 40.0),
                
                // University Road - 2 sensors
                createSensor("SENSOR-004", "University of Prishtina Main Gate", 42.6570, 21.1645, 
                        SensorType.COUNT, SensorStatus.ACTIVE, "ROAD-UNIVERSITY-01", 2, 30.0),
                createSensor("SENSOR-021", "University Road Exit", 42.6565, 21.1650, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-UNIVERSITY-01", 2, 30.0),
                
                // Newborn Monument area - 2 sensors
                createSensor("SENSOR-005", "Newborn Monument Junction", 42.6594, 21.1610, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-NEWBORN-01", 3, 40.0),
                createSensor("SENSOR-022", "Newborn Monument East", 42.6594, 21.1620, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-NEWBORN-01", 3, 40.0),
                
                // Government Quarter - 3 sensors for heavy traffic monitoring
                createSensor("SENSOR-006", "Government Buildings Roundabout", 42.6608, 21.1570, 
                        SensorType.RADAR, SensorStatus.ACTIVE, "ROAD-GOVT-01", 4, 50.0),
                createSensor("SENSOR-023", "Government Quarter Mid", 42.6605, 21.1580, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-GOVT-01", 4, 50.0),
                createSensor("SENSOR-024", "Government Quarter East", 42.6608, 21.1590, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-GOVT-01", 4, 50.0),
                
                // Central Bus Station area - 2 sensors
                createSensor("SENSOR-007", "Central Bus Station Entrance", 42.6632, 21.1655, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-BUS-STATION-01", 4, 40.0),
                createSensor("SENSOR-025", "Central Bus Station Exit", 42.6635, 21.1660, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-BUS-STATION-01", 4, 40.0),
                
                // Ring Road M9 - 3 sensors for highway monitoring
                createSensor("SENSOR-008", "Ring Road North M9 Entry", 42.6750, 21.1700, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-M9-NORTH-01", 4, 80.0),
                createSensor("SENSOR-026", "Ring Road North M9 Mid", 42.6760, 21.1710, 
                        SensorType.RADAR, SensorStatus.ACTIVE, "ROAD-M9-NORTH-01", 4, 80.0),
                createSensor("SENSOR-027", "Ring Road North M9 Exit", 42.6770, 21.1720, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-M9-NORTH-01", 4, 80.0),
                
                // Dragodan - 2 sensors
                createSensor("SENSOR-009", "Dragodan Main Intersection", 42.6520, 21.1720, 
                        SensorType.COUNT, SensorStatus.ACTIVE, "ROAD-DRAGODAN-01", 2, 40.0),
                createSensor("SENSOR-028", "Dragodan Secondary", 42.6515, 21.1725, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-DRAGODAN-01", 2, 40.0),
                
                // Ulpiana Shopping District - 2 sensors
                createSensor("SENSOR-010", "Ulpiana Shopping District North", 42.6550, 21.1580, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-ULPIANA-01", 3, 40.0),
                createSensor("SENSOR-029", "Ulpiana Shopping District South", 42.6545, 21.1585, 
                        SensorType.COUNT, SensorStatus.ACTIVE, "ROAD-ULPIANA-01", 3, 40.0),
                
                // Arbëria - 2 sensors
                createSensor("SENSOR-011", "Arbëria Main Road West", 42.6480, 21.1550, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-ARBERIA-01", 2, 50.0),
                createSensor("SENSOR-030", "Arbëria Main Road East", 42.6480, 21.1560, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-ARBERIA-01", 2, 50.0),
                
                // Germia Park Road - 2 sensors
                createSensor("SENSOR-012", "Germia Park Road Entrance", 42.6725, 21.1920, 
                        SensorType.COUNT, SensorStatus.ACTIVE, "ROAD-GERMIA-01", 2, 60.0),
                createSensor("SENSOR-031", "Germia Park Road Mid", 42.6730, 21.1930, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-GERMIA-01", 2, 60.0),
                
                // Mati 1 - 2 sensors
                createSensor("SENSOR-013", "Mati 1 Junction", 42.6560, 21.1850, 
                        SensorType.RADAR, SensorStatus.ACTIVE, "ROAD-MATI-01", 2, 40.0),
                createSensor("SENSOR-032", "Mati 1 Secondary", 42.6565, 21.1855, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-MATI-01", 2, 40.0),
                
                // Sunny Hill - 2 sensors
                createSensor("SENSOR-014", "Sunny Hill Main Street North", 42.6650, 21.1450, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-SUNNY-HILL-01", 3, 50.0),
                createSensor("SENSOR-033", "Sunny Hill Main Street South", 42.6645, 21.1455, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-SUNNY-HILL-01", 3, 50.0),
                
                // Veternik - 2 sensors
                createSensor("SENSOR-015", "Veternik Entry Point", 42.6800, 21.1600, 
                        SensorType.LOOP_DETECTOR, SensorStatus.ACTIVE, "ROAD-VETERNIK-01", 4, 60.0),
                createSensor("SENSOR-034", "Veternik Main Road", 42.6805, 21.1610, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-VETERNIK-01", 4, 60.0)
        );
    }

    private Sensor createSensor(String code, String name, double lat, double lon, 
                                SensorType type, SensorStatus status, 
                                String roadSegmentId, int laneCount, double speedLimit) {
        Sensor sensor = new Sensor();
        sensor.setCode(code);
        sensor.setName(name);
        
        Sensor.Location location = new Sensor.Location(lat, lon);
        location.setAddress(name + ", Sample City");
        sensor.setLocation(location);
        
        sensor.setType(type);
        sensor.setStatus(status);
        sensor.setRoadSegmentId(roadSegmentId);
        sensor.setLaneCount(laneCount);
        sensor.setSpeedLimit(speedLimit);
        
        return sensor;
    }
}
