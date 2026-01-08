package com.cityflow.route;

import com.cityflow.route.dto.StopRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.enabled=false")
@AutoConfigureMockMvc
class StopControllerIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("cityflow")
                    .withUsername("kledionberisha")
                    .withPassword("kledion123");

    private static final boolean DOCKER_AVAILABLE = DockerClientFactory.instance().isDockerAvailable();

    @BeforeAll
    static void startContainerIfDockerAvailable() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker is required for Testcontainers");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres.isRunning()) {
            postgres.stop();
        }
    }

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker is required for Testcontainers");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private StopRequest stopReq(String code, String name) {
        StopRequest req = new StopRequest();
        req.setCode(code);
        req.setName(name);
        req.setLatitude(BigDecimal.valueOf(41.3275));
        req.setLongitude(BigDecimal.valueOf(19.8187));
        req.setTerminal(false);
        req.setActive(true);
        return req;
    }

    @Test
    void createListAndFetchStop() throws Exception {
        StopRequest req = stopReq("S1", "Central");

        String location = mockMvc.perform(post("/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S1"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(get("/stops?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("S1"));
    }

    @Test
    void duplicateCodeReturnsConflict() throws Exception {
        StopRequest req = stopReq("S2", "Square");

        mockMvc.perform(post("/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidLatLonReturnsBadRequest() throws Exception {
        StopRequest req = stopReq("S3", "BadCoords");
        req.setLatitude(BigDecimal.valueOf(200));

        mockMvc.perform(post("/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
