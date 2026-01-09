package com.cityflow.analytics.consumer;

import com.cityflow.analytics.dto.TrafficReadingEvent;
import com.cityflow.analytics.service.AnalyticsAggregationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TrafficEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficEventConsumer.class);

    private final AnalyticsAggregationService aggregationService;
    private final ObjectMapper objectMapper;

    public TrafficEventConsumer(AnalyticsAggregationService aggregationService, ObjectMapper objectMapper) {
        this.aggregationService = aggregationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.traffic-readings:traffic.reading.events}",
            groupId = "${app.kafka.consumer.group-id:analytics-service}",
            concurrency = "3"
    )
    public void consumeTrafficReading(String message) {
        try {
            TrafficReadingEvent event = objectMapper.readValue(message, TrafficReadingEvent.class);
            log.debug("Received traffic reading event for sensor: {}, congestion: {}", 
                    event.getSensorCode(), event.getCongestionLevel());
            
            aggregationService.processTrafficReading(event);
        } catch (Exception e) {
            log.error("Failed to process traffic reading event", e);
        }
    }
}
