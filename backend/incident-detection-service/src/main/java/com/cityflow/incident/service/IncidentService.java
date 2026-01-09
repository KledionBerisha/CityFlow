package com.cityflow.incident.service;

import com.cityflow.incident.dto.IncidentResponse;
import com.cityflow.incident.model.Incident;
import com.cityflow.incident.model.IncidentSeverity;
import com.cityflow.incident.model.IncidentStatus;
import com.cityflow.incident.model.IncidentType;
import com.cityflow.incident.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public Flux<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll()
                .map(this::toResponse);
    }

    public Mono<IncidentResponse> getIncidentById(String id) {
        return incidentRepository.findById(id)
                .map(this::toResponse);
    }

    public Flux<IncidentResponse> getIncidentsByStatus(IncidentStatus status) {
        return incidentRepository.findByStatus(status)
                .map(this::toResponse);
    }

    public Flux<IncidentResponse> getIncidentsByType(IncidentType type) {
        return incidentRepository.findByType(type)
                .map(this::toResponse);
    }

    public Flux<IncidentResponse> getIncidentsBySeverity(IncidentSeverity severity) {
        return incidentRepository.findBySeverity(severity)
                .map(this::toResponse);
    }

    public Flux<IncidentResponse> getActiveIncidents() {
        return incidentRepository.findByStatus(IncidentStatus.DETECTED)
                .concatWith(incidentRepository.findByStatus(IncidentStatus.CONFIRMED))
                .concatWith(incidentRepository.findByStatus(IncidentStatus.IN_PROGRESS))
                .map(this::toResponse);
    }

    public Flux<IncidentResponse> getRecentIncidents(int hoursBack) {
        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);
        return incidentRepository.findByDetectedAtAfter(since)
                .map(this::toResponse);
    }

    public Mono<IncidentResponse> updateIncidentStatus(String id, IncidentStatus newStatus) {
        return incidentRepository.findById(id)
                .flatMap(incident -> {
                    incident.setStatus(newStatus);
                    incident.setUpdatedAt(Instant.now());
                    
                    if (newStatus == IncidentStatus.CONFIRMED) {
                        incident.setConfirmedAt(Instant.now());
                    } else if (newStatus == IncidentStatus.RESOLVED) {
                        incident.setResolvedAt(Instant.now());
                    }
                    
                    return incidentRepository.save(incident);
                })
                .map(this::toResponse);
    }

    public Mono<Void> deleteIncident(String id) {
        return incidentRepository.deleteById(id);
    }

    private IncidentResponse toResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .incidentCode(incident.getIncidentCode())
                .type(incident.getType())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .latitude(incident.getLatitude())
                .longitude(incident.getLongitude())
                .roadSegmentId(incident.getRoadSegmentId())
                .address(incident.getAddress())
                .sourceId(incident.getSourceId())
                .sourceType(incident.getSourceType())
                .relatedSourceIds(incident.getRelatedSourceIds())
                .detectedAt(incident.getDetectedAt())
                .confirmedAt(incident.getConfirmedAt())
                .resolvedAt(incident.getResolvedAt())
                .detectionMethod(incident.getDetectionMethod())
                .confidence(incident.getConfidence())
                .impactRadiusKm(incident.getImpactRadiusKm())
                .affectedVehicles(incident.getAffectedVehicles())
                .affectedBuses(incident.getAffectedBuses())
                .estimatedDelayMinutes(incident.getEstimatedDelayMinutes())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
