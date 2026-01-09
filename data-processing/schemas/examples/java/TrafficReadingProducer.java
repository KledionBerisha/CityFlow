package com.cityflow.examples;

import com.cityflow.events.traffic.TrafficReadingEvent;
import com.cityflow.events.traffic.CongestionLevel;
import com.cityflow.events.traffic.GeoLocation;
import com.cityflow.events.traffic.WeatherCondition;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Example Kafka producer that sends TrafficReadingEvent messages
 * using Avro serialization and Schema Registry
 */
public class TrafficReadingProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(TrafficReadingProducer.class);
    
    private static final String TOPIC = "traffic.reading.events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9094";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    
    public static void main(String[] args) {
        Properties props = createProducerConfig();
        
        try (KafkaProducer<String, TrafficReadingEvent> producer = new KafkaProducer<>(props)) {
            logger.info("Starting Traffic Reading Producer...");
            
            // Simulate sending 10 traffic readings
            for (int i = 0; i < 10; i++) {
                TrafficReadingEvent event = createSampleEvent(i);
                
                ProducerRecord<String, TrafficReadingEvent> record = 
                    new ProducerRecord<>(TOPIC, event.getSensorCode().toString(), event);
                
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Error sending message", exception);
                    } else {
                        logger.info("Sent event to partition {} with offset {}", 
                            metadata.partition(), metadata.offset());
                    }
                });
                
                Thread.sleep(1000); // Wait 1 second between messages
            }
            
            producer.flush();
            logger.info("All messages sent successfully!");
            
        } catch (Exception e) {
            logger.error("Error in producer", e);
        }
    }
    
    private static Properties createProducerConfig() {
        Properties props = new Properties();
        
        // Kafka broker configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "traffic-producer-example");
        
        // Serializer configuration
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        
        // Schema Registry configuration
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        props.put("auto.register.schemas", "true");
        props.put("use.latest.version", "true");
        
        // Performance tuning
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        return props;
    }
    
    private static TrafficReadingEvent createSampleEvent(int index) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Create location
        GeoLocation location = GeoLocation.newBuilder()
            .setLatitude(42.6521 + (random.nextDouble() * 0.01))
            .setLongitude(21.1657 + (random.nextDouble() * 0.01))
            .build();
        
        // Determine congestion level based on speed
        double speed = 20 + random.nextDouble() * 80; // 20-100 km/h
        CongestionLevel congestionLevel = determineCongestionLevel(speed);
        
        // Create metadata
        Map<CharSequence, CharSequence> metadata = new HashMap<>();
        metadata.put("source", "simulator");
        metadata.put("version", "1.0");
        
        // Build the event
        return TrafficReadingEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setEventType("TRAFFIC_READING")
            .setTimestamp(Instant.now().toEpochMilli())
            .setSensorId("sensor-id-" + (index + 1))
            .setSensorCode("SENSOR-" + String.format("%03d", index + 1))
            .setRoadSegmentId("ROAD-SEG-" + String.format("%03d", (index % 5) + 1))
            .setLocation(location)
            .setAverageSpeed(speed)
            .setVehicleCount(random.nextInt(10, 100))
            .setOccupancy(random.nextDouble())
            .setCongestionLevel(congestionLevel)
            .setQueueLength(congestionLevel == CongestionLevel.SEVERE ? random.nextInt(20, 50) : random.nextInt(0, 10))
            .setTemperature(15 + random.nextDouble() * 15) // 15-30°C
            .setWeatherCondition(WeatherCondition.CLEAR)
            .setIncidentDetected(random.nextDouble() < 0.1) // 10% chance of incident
            .setMetadata(metadata)
            .build();
    }
    
    private static CongestionLevel determineCongestionLevel(double speed) {
        if (speed >= 80) return CongestionLevel.FREE_FLOW;
        if (speed >= 60) return CongestionLevel.LIGHT;
        if (speed >= 40) return CongestionLevel.MODERATE;
        if (speed >= 20) return CongestionLevel.HEAVY;
        return CongestionLevel.SEVERE;
    }
}
