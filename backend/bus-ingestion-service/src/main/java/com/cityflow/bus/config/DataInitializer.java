package com.cityflow.bus.config;

import com.cityflow.bus.model.Bus;
import com.cityflow.bus.repository.BusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Configuration
public class DataInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initializeBuses(BusRepository busRepository) {
        return args -> {
            // Check if buses already exist
            busRepository.count()
                    .flatMap(count -> {
                        if (count > 0) {
                            log.info("Database already contains {} buses, skipping initialization", count);
                            return Mono.empty();
                        }

                        log.info("Initializing sample bus data...");

                        List<Bus> sampleBuses = List.of(
                                Bus.builder()
                                        .vehicleId("BUS-001")
                                        .licensePlate("CF-001-TI")
                                        .status(Bus.BusStatus.ACTIVE)
                                        .capacity(50)
                                        .model("Mercedes-Benz Citaro")
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build(),
                                Bus.builder()
                                        .vehicleId("BUS-002")
                                        .licensePlate("CF-002-TI")
                                        .status(Bus.BusStatus.ACTIVE)
                                        .capacity(50)
                                        .model("Mercedes-Benz Citaro")
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build(),
                                Bus.builder()
                                        .vehicleId("BUS-003")
                                        .licensePlate("CF-003-TI")
                                        .status(Bus.BusStatus.ACTIVE)
                                        .capacity(45)
                                        .model("Volvo 7900")
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build(),
                                Bus.builder()
                                        .vehicleId("BUS-004")
                                        .licensePlate("CF-004-TI")
                                        .status(Bus.BusStatus.IDLE)
                                        .capacity(45)
                                        .model("Volvo 7900")
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build(),
                                Bus.builder()
                                        .vehicleId("BUS-005")
                                        .licensePlate("CF-005-TI")
                                        .status(Bus.BusStatus.ACTIVE)
                                        .capacity(60)
                                        .model("MAN Lion's City")
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build()
                        );

                        return busRepository.saveAll(sampleBuses)
                                .collectList()
                                .doOnSuccess(buses -> 
                                        log.info("Successfully initialized {} sample buses", buses.size()));
                    })
                    .subscribe();
        };
    }
}
