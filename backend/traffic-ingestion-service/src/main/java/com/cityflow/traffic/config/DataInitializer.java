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
        return Arrays.asList(
                createSensor("SENSOR-001", "Main Street North", 42.3601, -71.0589, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-SEG-001", 4, 60.0),
                
                createSensor("SENSOR-002", "Main Street South", 42.3585, -71.0602, 
                        SensorType.SPEED, SensorStatus.ACTIVE, "ROAD-SEG-002", 4, 60.0),
                
                createSensor("SENSOR-003", "Highway 101 Entry", 42.3650, -71.0550, 
                        SensorType.LOOP_DETECTOR, SensorStatus.ACTIVE, "ROAD-SEG-003", 6, 100.0),
                
                createSensor("SENSOR-004", "Downtown Junction", 42.3612, -71.0571, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-SEG-004", 3, 50.0),
                
                createSensor("SENSOR-005", "Industrial Zone", 42.3640, -71.0620, 
                        SensorType.COUNT, SensorStatus.ACTIVE, "ROAD-SEG-005", 2, 40.0),
                
                createSensor("SENSOR-006", "University Avenue", 42.3575, -71.0640, 
                        SensorType.RADAR, SensorStatus.ACTIVE, "ROAD-SEG-006", 4, 50.0),
                
                createSensor("SENSOR-007", "Airport Road", 42.3670, -71.0500, 
                        SensorType.SPEED, SensorStatus.INACTIVE, "ROAD-SEG-007", 4, 80.0),
                
                createSensor("SENSOR-008", "Shopping District", 42.3590, -71.0580, 
                        SensorType.CAMERA, SensorStatus.ACTIVE, "ROAD-SEG-008", 2, 40.0)
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
