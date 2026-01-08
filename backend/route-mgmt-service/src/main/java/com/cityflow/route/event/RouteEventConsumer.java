package com.cityflow.route.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RouteEventConsumer.class);

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @KafkaListener(topics = "${app.kafka.topics.route-events:route.events}",
            containerFactory = "routeEventKafkaListenerContainerFactory",
            autoStartup = "${app.kafka.enabled:true}")
    public void handle(List<RouteEvent> events) {
        if (!kafkaEnabled) {
            return;
        }
        for (RouteEvent evt : events) {
            log.info("Consumed route event type={} code={} id={} active={}",
                    evt.getType(), evt.getCode(), evt.getId(), evt.isActive());
        }
    }
}
