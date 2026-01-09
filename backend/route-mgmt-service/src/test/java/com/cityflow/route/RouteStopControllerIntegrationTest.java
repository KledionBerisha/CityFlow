package com.cityflow.route;

import com.cityflow.route.dto.RouteRequest;
import com.cityflow.route.dto.RouteStopRequest;
import com.cityflow.route.dto.ScheduleRequest;
import com.cityflow.route.dto.StopRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.enabled=false",
        "app.kafka.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class RouteStopControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("cityflow")
                    .withUsername("cityflow")
                    .withPassword("cityflow");

    @DynamicPropertySource
    static void dataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private RouteRequest routeReq(String code, String name) {
        RouteRequest req = new RouteRequest();
        req.setCode(code);
        req.setName(name);
        req.setActive(true);
        return req;
    }

    private StopRequest stopReq(String code, String name, double lat, double lon) {
        StopRequest req = new StopRequest();
        req.setCode(code);
        req.setName(name);
        req.setLatitude(BigDecimal.valueOf(lat));
        req.setLongitude(BigDecimal.valueOf(lon));
        req.setTerminal(false);
        req.setActive(true);
        return req;
    }

    private UUID createRoute(String code) throws Exception {
        String body = objectMapper.writeValueAsString(routeReq(code, "Route " + code));
        String id = mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(id).get("id").traverse().readValueAs(UUID.class);
    }

    private UUID createStop(String code, double lat, double lon) throws Exception {
        String body = objectMapper.writeValueAsString(stopReq(code, "Stop " + code, lat, lon));
        String id = mockMvc.perform(post("/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(id).get("id").traverse().readValueAs(UUID.class);
    }

    @Test
    void replaceAndListRouteStops() throws Exception {
        UUID routeId = createRoute("RS1");
        UUID stop1 = createStop("ST1", 41.3275, 19.8187);
        UUID stop2 = createStop("ST2", 41.3300, 19.8200);

        List<RouteStopRequest> payload = List.of(
                rs(stop1, 1),
                rs(stop2, 2)
        );

        mockMvc.perform(put("/routes/" + routeId + "/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sequenceOrder").value(1))
                .andExpect(jsonPath("$[1].sequenceOrder").value(2));

        mockMvc.perform(get("/routes/" + routeId + "/stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stopCode").value("ST1"))
                .andExpect(jsonPath("$[1].stopCode").value("ST2"));
    }

    @Test
    void duplicateSequenceReturnsBadRequest() throws Exception {
        UUID routeId = createRoute("RS2");
        UUID stop1 = createStop("ST3", 41.1, 19.1);
        UUID stop2 = createStop("ST4", 41.2, 19.2);

        List<RouteStopRequest> payload = List.of(
                rs(stop1, 1),
                rs(stop2, 1)
        );

        mockMvc.perform(put("/routes/" + routeId + "/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaceSchedules() throws Exception {
        UUID routeId = createRoute("RS3");
        List<ScheduleRequest> payload = List.of(
                sched((short) 1, 10),
                sched((short) 2, 15)
        );

        mockMvc.perform(put("/routes/" + routeId + "/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$[0].frequencyMin").value(10));

        mockMvc.perform(get("/routes/" + routeId + "/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].dayOfWeek").value(2))
                .andExpect(jsonPath("$[1].frequencyMin").value(15));
    }

    private RouteStopRequest rs(UUID stopId, int seq) {
        RouteStopRequest r = new RouteStopRequest();
        r.setStopId(stopId);
        r.setSequenceOrder(seq);
        return r;
    }

    private ScheduleRequest sched(short dow, int freq) {
        ScheduleRequest s = new ScheduleRequest();
        s.setDayOfWeek(dow);
        s.setFrequencyMin(freq);
        return s;
    }
}
