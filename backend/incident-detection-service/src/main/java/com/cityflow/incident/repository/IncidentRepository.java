package com.cityflow.incident.repository;

import com.cityflow.incident.model.Incident;
import com.cityflow.incident.model.IncidentSeverity;
import com.cityflow.incident.model.IncidentStatus;
import com.cityflow.incident.model.IncidentType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface IncidentRepository extends ReactiveMongoRepository<Incident, String> {
    
    Flux<Incident> findByStatus(IncidentStatus status);
    
    Flux<Incident> findByType(IncidentType type);
    
    Flux<Incident> findBySeverity(IncidentSeverity severity);
    
    Flux<Incident> findByRoadSegmentIdAndStatusNot(String roadSegmentId, IncidentStatus status);
    
    Flux<Incident> findByDetectedAtAfter(Instant after);
    
    Flux<Incident> findByStatusAndDetectedAtAfter(IncidentStatus status, Instant after);
    
    Flux<Incident> findByTypeAndStatusOrderByDetectedAtDesc(IncidentType type, IncidentStatus status);
}
