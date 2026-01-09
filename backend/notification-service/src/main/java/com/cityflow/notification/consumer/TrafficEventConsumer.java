package com.cityflow.notification.consumer;

import com.cityflow.notification.dto.TrafficReadingEvent;
import com.cityflow.notification.service.AlertProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TrafficEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficEventConsumer.class);

    private final AlertProcessingService alertProcessingService;
    private final ObjectMapper objectMapper;

    public TrafficEventConsumer(AlertProcessingService alertProcessingService, ObjectMapper objectMapper) {
        this.alertProcessingService = alertProcessingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.traffic-readings:traffic.reading.events}",
            groupId = "${app.kafka.consumer.group-id:notification-service}",
            concurrency = "2"
    )
    public void consumeTrafficReading(String message) {
        try {
            TrafficReadingEvent event = objectMapper.readValue(message, TrafficReadingEvent.class);
            log.debug("Processing traffic event: sensor={}, congestion={}", 
                    event.getSensorCode(), event.getCongestionLevel());
            
            alertProcessingService.processTrafficEvent(event);
        } catch (Exception e) {
            log.error("Failed to process traffic event", e);
        }
    }
}
