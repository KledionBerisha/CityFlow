package com.cityflow.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableReactiveMongoAuditing
public class IncidentDetectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentDetectionServiceApplication.class, args);
    }
}
