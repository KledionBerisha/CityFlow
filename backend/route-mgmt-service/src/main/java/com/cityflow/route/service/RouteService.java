package com.cityflow.route.service;

import com.cityflow.route.dto.RouteRequest;
import com.cityflow.route.dto.RouteResponse;
import com.cityflow.route.event.RouteEvent;
import com.cityflow.route.model.Route;
import com.cityflow.route.repository.RouteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${app.kafka.topics.route-events:route.events}")
    private String routeEventsTopic;

    public RouteService(RouteRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public RouteResponse create(RouteRequest req) {
        String code = safeTrim(req.getCode());
        String name = safeTrim(req.getName());

        if (code.isEmpty() || name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route code and name are required");
        }

        if (repository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Route code already exists");
        }
        Route route = new Route();
        route.setCode(code);
        route.setName(name);
        route.setActive(req.isActive());
        Route saved = repository.save(route);
        publishRouteEvent(saved, "ROUTE_CREATED");
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> list(Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return repository.findAll(capped).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public RouteResponse get(UUID id) {
        Route r = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
        return toDto(r);
    }

    private RouteResponse toDto(Route r) {
        RouteResponse dto = new RouteResponse();
        dto.setId(r.getId());
        dto.setCode(r.getCode());
        dto.setName(r.getName());
        dto.setActive(r.isActive());
        return dto;
    }

    private void publishRouteEvent(Route route, String type) {
        if (!kafkaEnabled) {
            return;
        }
        try {
            RouteEvent evt = new RouteEvent();
            evt.setId(route.getId());
            evt.setCode(route.getCode());
            evt.setName(route.getName());
            evt.setActive(route.isActive());
            evt.setType(type);
            evt.setOccurredAt(java.time.Instant.now());
            kafkaTemplate.send(routeEventsTopic, route.getId().toString(), evt)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish route event type={} id={}: {}", type, route.getId(), ex.toString());
                        } else if (log.isDebugEnabled()) {
                            log.debug("Published route event type={} id={} partition={} offset={}",
                                    type, route.getId(), res.getRecordMetadata().partition(),
                                    res.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            // Non-fatal publish failure
            log.warn("Failed to publish route event type={} id={}: {}", type, route.getId(), ex.toString());
        }
    }

    private Pageable capPageable(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
