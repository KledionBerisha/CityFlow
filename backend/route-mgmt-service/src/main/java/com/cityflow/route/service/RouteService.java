package com.cityflow.route.service;

import com.cityflow.route.dto.RouteRequest;
import com.cityflow.route.dto.RouteResponse;
import com.cityflow.route.model.Route;
import com.cityflow.route.repository.RouteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {

    private final RouteRepository repository;

    public RouteService(RouteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RouteResponse create(RouteRequest req) {
        if (repository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Route code already exists");
        }
        Route route = new Route();
        route.setCode(req.getCode());
        route.setName(req.getName());
        route.setActive(req.isActive());
        Route saved = repository.save(route);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse get(UUID id) {
        Route r = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));
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
}
