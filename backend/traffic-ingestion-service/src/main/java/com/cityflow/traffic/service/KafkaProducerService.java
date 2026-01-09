package com.cityflow.traffic.service;

import com.cityflow.traffic.event.SensorStatusEvent;
import com.cityflow.traffic.event.TrafficReadingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean kafkaEnabled;

    @Value("${app.kafka.topics.traffic-readings:traffic.reading.events}")
    private String trafficReadingsTopic;

    @Value("${app.kafka.topics.sensor-status:sensor.status.events}")
    private String sensorStatusTopic;

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publishTrafficReadingEvent(TrafficReadingEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping traffic reading event publication");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(trafficReadingsTopic, event.getSensorId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish traffic reading event for sensor {}", 
                                    event.getSensorCode(), ex);
                        } else {
                            log.debug("Published traffic reading event for sensor {} to partition {}", 
                                    event.getSensorCode(), result.getRecordMetadata().partition());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize traffic reading event", e);
        }
    }

    public void publishSensorStatusEvent(SensorStatusEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping sensor status event publication");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(sensorStatusTopic, event.getSensorId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish sensor status event for sensor {}", 
                                    event.getSensorCode(), ex);
                        } else {
                            log.debug("Published sensor status event for sensor {}: {} -> {}", 
                                    event.getSensorCode(), event.getOldStatus(), event.getNewStatus());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sensor status event", e);
        }
    }
}
