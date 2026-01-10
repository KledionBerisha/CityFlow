package com.cityflow.car.service;

import com.cityflow.car.event.CarLocationEvent;
import com.cityflow.car.event.CarStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String carLocationTopic;
    private final String carStatusTopic;

    public KafkaProducerService(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:true}") boolean kafkaEnabled,
            @Value("${app.kafka.topics.car-location}") String carLocationTopic,
            @Value("${app.kafka.topics.car-status}") String carStatusTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
        this.carLocationTopic = carLocationTopic;
        this.carStatusTopic = carStatusTopic;
    }

    /**
     * Publish car location event to Kafka
     */
    public void publishLocationEvent(CarLocationEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping location event for car: {}", event.getCarId());
            return;
        }

        try {
            kafkaTemplate.send(carLocationTopic, event.getCarId(), event)
                    .thenApply(result -> {
                        log.debug("Published location event for car {} to topic {}", 
                                event.getCarId(), carLocationTopic);
                        return result;
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish location event for car {}", event.getCarId(), ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error publishing location event", e);
        }
    }

    /**
     * Publish car status change event to Kafka
     */
    public void publishStatusEvent(CarStatusEvent event) {
        if (!kafkaEnabled) {
            log.debug("Kafka disabled, skipping status event for car: {}", event.getCarId());
            return;
        }

        try {
            kafkaTemplate.send(carStatusTopic, event.getCarId(), event)
                    .thenApply(result -> {
                        log.debug("Published status event for car {} to topic {}", 
                                event.getCarId(), carStatusTopic);
                        return result;
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to publish status event for car {}", event.getCarId(), ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error publishing status event", e);
        }
    }
}

