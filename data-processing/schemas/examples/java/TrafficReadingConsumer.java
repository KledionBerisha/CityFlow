package com.cityflow.examples;

import com.cityflow.events.traffic.TrafficReadingEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;

/**
 * Example Kafka consumer that reads TrafficReadingEvent messages
 * using Avro deserialization and Schema Registry
 */
public class TrafficReadingConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TrafficReadingConsumer.class);
    
    private static final String TOPIC = "traffic.reading.events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9094";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String GROUP_ID = "traffic-consumer-example";
    
    public static void main(String[] args) {
        Properties props = createConsumerConfig();
        
        try (KafkaConsumer<String, TrafficReadingEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));
            logger.info("Started Traffic Reading Consumer. Waiting for messages...");
            
            while (true) {
                ConsumerRecords<String, TrafficReadingEvent> records = 
                    consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, TrafficReadingEvent> record : records) {
                    processEvent(record);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in consumer", e);
        }
    }
    
    private static Properties createConsumerConfig() {
        Properties props = new Properties();
        
        // Kafka broker configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "traffic-consumer-example-" + System.currentTimeMillis());
        
        // Deserializer configuration
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        
        // Schema Registry configuration
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
        
        // Consumer behavior
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        return props;
    }
    
    private static void processEvent(ConsumerRecord<String, TrafficReadingEvent> record) {
        TrafficReadingEvent event = record.value();
        
        logger.info("========================================");
        logger.info("Received Traffic Reading Event:");
        logger.info("  Partition: {}, Offset: {}", record.partition(), record.offset());
        logger.info("  Event ID: {}", event.getEventId());
        logger.info("  Sensor: {} ({})", event.getSensorCode(), event.getSensorId());
        logger.info("  Road Segment: {}", event.getRoadSegmentId());
        logger.info("  Location: ({}, {})", 
            event.getLocation().getLatitude(), 
            event.getLocation().getLongitude());
        logger.info("  Average Speed: {} km/h", event.getAverageSpeed());
        logger.info("  Vehicle Count: {}", event.getVehicleCount());
        logger.info("  Occupancy: {}", String.format("%.2f%%", event.getOccupancy() * 100));
        logger.info("  Congestion Level: {}", event.getCongestionLevel());
        logger.info("  Queue Length: {} vehicles", event.getQueueLength());
        logger.info("  Temperature: {}°C", String.format("%.1f", event.getTemperature()));
        logger.info("  Weather: {}", event.getWeatherCondition());
        logger.info("  Incident Detected: {}", event.getIncidentDetected());
        logger.info("  Timestamp: {}", Instant.ofEpochMilli(event.getTimestamp()));
        
        if (event.getMetadata() != null) {
            logger.info("  Metadata: {}", event.getMetadata());
        }
        
        // Business logic examples
        detectCongestion(event);
        checkForIncidents(event);
    }
    
    private static void detectCongestion(TrafficReadingEvent event) {
        if (event.getCongestionLevel().toString().equals("SEVERE") || 
            event.getCongestionLevel().toString().equals("HEAVY")) {
            logger.warn("⚠️  ALERT: Heavy congestion detected on {} - Speed: {} km/h",
                event.getRoadSegmentId(), event.getAverageSpeed());
        }
    }
    
    private static void checkForIncidents(TrafficReadingEvent event) {
        if (event.getIncidentDetected()) {
            logger.error("🚨 INCIDENT ALERT: Possible incident at {} ({}, {})",
                event.getRoadSegmentId(),
                event.getLocation().getLatitude(),
                event.getLocation().getLongitude());
        }
    }
}
