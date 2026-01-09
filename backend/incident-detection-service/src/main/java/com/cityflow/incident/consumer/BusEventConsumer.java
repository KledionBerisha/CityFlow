package com.cityflow.incident.consumer;

import com.cityflow.incident.dto.BusLocationEvent;
import com.cityflow.incident.service.IncidentDetectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class BusEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BusEventConsumer.class);

    private final IncidentDetectionService detectionService;
    private final ObjectMapper objectMapper;

    public BusEventConsumer(IncidentDetectionService detectionService, ObjectMapper objectMapper) {
        this.detectionService = detectionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.bus-location:bus.location.events}",
                   groupId = "${app.kafka.consumer.group-id:incident-detection-service}",
                   concurrency = "2")
    public void consumeBusLocation(String message) {
        try {
            BusLocationEvent event = objectMapper.readValue(message, BusLocationEvent.class);
            log.debug("Consumed bus location: {}", event.getVehicleId());
            
            detectionService.analyzeBusLocation(event).subscribe();
            
        } catch (Exception e) {
            log.error("Error processing bus location event: {}", e.getMessage(), e);
        }
    }
}
