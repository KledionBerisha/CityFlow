package com.cityflow.route.config;

import com.cityflow.route.event.RouteEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    @Value("${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9093}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:route-mgmt-service}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, RouteEvent> routeEventConsumerFactory() {
        JsonDeserializer<RouteEvent> jsonDeserializer = new JsonDeserializer<>(RouteEvent.class);
        jsonDeserializer.addTrustedPackages("com.cityflow.route.event");
        jsonDeserializer.setUseTypeMapperForKey(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RouteEvent> routeEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RouteEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(routeEventConsumerFactory());
        factory.setBatchListener(true);
        return factory;
    }
}
