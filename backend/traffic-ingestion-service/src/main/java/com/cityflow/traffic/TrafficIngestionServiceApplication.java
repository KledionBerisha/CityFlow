package com.cityflow.traffic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrafficIngestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficIngestionServiceApplication.class, args);
    }
}
