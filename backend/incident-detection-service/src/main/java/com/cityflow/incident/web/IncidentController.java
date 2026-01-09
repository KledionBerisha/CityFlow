package com.cityflow.incident.web;

import com.cityflow.incident.dto.IncidentResponse;
import com.cityflow.incident.model.IncidentSeverity;
import com.cityflow.incident.model.IncidentStatus;
import com.cityflow.incident.model.IncidentType;
import com.cityflow.incident.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public Flux<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/{id}")
    public Mono<IncidentResponse> getIncidentById(@PathVariable String id) {
        return incidentService.getIncidentById(id);
    }

    @GetMapping("/active")
    public Flux<IncidentResponse> getActiveIncidents() {
        return incidentService.getActiveIncidents();
    }

    @GetMapping("/recent")
    public Flux<IncidentResponse> getRecentIncidents(
            @RequestParam(defaultValue = "24") int hoursBack) {
        return incidentService.getRecentIncidents(hoursBack);
    }

    @GetMapping("/status/{status}")
    public Flux<IncidentResponse> getIncidentsByStatus(@PathVariable IncidentStatus status) {
        return incidentService.getIncidentsByStatus(status);
    }

    @GetMapping("/type/{type}")
    public Flux<IncidentResponse> getIncidentsByType(@PathVariable IncidentType type) {
        return incidentService.getIncidentsByType(type);
    }

    @GetMapping("/severity/{severity}")
    public Flux<IncidentResponse> getIncidentsBySeverity(@PathVariable IncidentSeverity severity) {
        return incidentService.getIncidentsBySeverity(severity);
    }

    @PatchMapping("/{id}/status")
    public Mono<IncidentResponse> updateIncidentStatus(
            @PathVariable String id,
            @RequestParam IncidentStatus status) {
        return incidentService.updateIncidentStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteIncident(@PathVariable String id) {
        return incidentService.deleteIncident(id);
    }
}
