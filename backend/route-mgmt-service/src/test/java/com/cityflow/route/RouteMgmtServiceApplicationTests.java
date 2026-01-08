package com.cityflow.route;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/cityflow",
        "spring.datasource.username=kledionberisha",
        "spring.datasource.password=kledion123"
})
class RouteMgmtServiceApplicationTests {

    @Test
    void contextLoads() {
        // context startup verifies wiring and Flyway migration against containerized Postgres
    }
}
