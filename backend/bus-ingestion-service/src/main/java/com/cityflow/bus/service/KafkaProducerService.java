package com.cityflow.bus.service;

import com.cityflow.bus.event.BusLocationEvent;
import com.cityflow.bus.event.BusStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String busLocationTopic;
    private final String busStatusTopic;

    public KafkaProducerService(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:true}") boolean kafkaEnabled,
            @Value("${app.kafka.topics.bus-location}") String busLocationTopic,
            @Value("${app.kafka.topics.bus-status}") String busStatusTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
        this.busLocationTopic = busLocationTopic;
        this.busStatusTopic = busStatusTopic;
    }

    /**
     * Publish bus location event to Kafka
     */
    public void publishLocationEvent(BusLocationEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping location event for bus: {}", event.getBusId());
            return;
        }

        try {
            kafkaTemplate.send(busLocationTopic, event.getBusId(), event)
                    .thenApply(result -> {
                        log.debug("Published location event for bus {} to topic {}", 
                                event.getBusId(), busLocationTopic);
                        return result;
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish location event for bus {}", event.getBusId(), ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error publishing location event", e);
        }
    }

    /**
     * Publish bus status change event to Kafka
     */
    public void publishStatusEvent(BusStatusEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping status event for bus: {}", event.getBusId());
            return;
        }

        try {
            kafkaTemplate.send(busStatusTopic, event.getBusId(), event)
                    .thenApply(result -> {
                        log.debug("Published status event for bus {} to topic {}", 
                                event.getBusId(), busStatusTopic);
                        return result;
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish status event for bus {}", event.getBusId(), ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error publishing status event", e);
        }
    }
}
