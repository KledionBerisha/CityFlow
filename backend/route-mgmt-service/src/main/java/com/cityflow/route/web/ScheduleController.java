package com.cityflow.route.web;

import com.cityflow.route.dto.ScheduleRequest;
import com.cityflow.route.dto.ScheduleResponse;
import com.cityflow.route.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/routes/{routeId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleResponse> list(@PathVariable("routeId") UUID routeId) {
        return scheduleService.list(routeId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ScheduleResponse> replace(@PathVariable("routeId") UUID routeId,
                                          @Valid @RequestBody List<ScheduleRequest> payload) {
        return scheduleService.replace(routeId, payload);
    }
}
