package com.cityflow.analytics.consumer;

import com.cityflow.analytics.dto.BusLocationEvent;
import com.cityflow.analytics.service.AnalyticsAggregationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BusEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BusEventConsumer.class);

    private final AnalyticsAggregationService aggregationService;
    private final ObjectMapper objectMapper;

    public BusEventConsumer(AnalyticsAggregationService aggregationService, ObjectMapper objectMapper) {
        this.aggregationService = aggregationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.bus-locations:bus.location.events}",
            groupId = "${app.kafka.consumer.group-id:analytics-service}",
            concurrency = "3"
    )
    public void consumeBusLocation(String message) {
        try {
            BusLocationEvent event = objectMapper.readValue(message, BusLocationEvent.class);
            log.debug("Received bus location event for bus: {}, route: {}", 
                    event.getBusCode(), event.getRouteId());
            
            aggregationService.processBusLocation(event);
        } catch (Exception e) {
            log.error("Failed to process bus location event", e);
        }
    }
}
