package com.cityflow.incident.consumer;

import com.cityflow.incident.dto.TrafficReadingEvent;
import com.cityflow.incident.service.IncidentDetectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TrafficEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficEventConsumer.class);

    private final IncidentDetectionService detectionService;
    private final ObjectMapper objectMapper;

    public TrafficEventConsumer(IncidentDetectionService detectionService, ObjectMapper objectMapper) {
        this.detectionService = detectionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.traffic-reading:traffic.reading.events}",
                   groupId = "${app.kafka.consumer.group-id:incident-detection-service}",
                   concurrency = "3")
    public void consumeTrafficReading(String message) {
        try {
            TrafficReadingEvent event = objectMapper.readValue(message, TrafficReadingEvent.class);
            log.debug("Consumed traffic reading from sensor: {}", event.getSensorCode());
            
            detectionService.analyzeTrafficReading(event).subscribe();
            
        } catch (Exception e) {
            log.error("Error processing traffic reading event: {}", e.getMessage(), e);
        }
    }
}
