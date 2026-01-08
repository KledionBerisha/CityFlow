package com.cityflow.route.service;

import com.cityflow.route.dto.RouteRequest;
import com.cityflow.route.dto.RouteResponse;
import com.cityflow.route.model.Route;
import com.cityflow.route.repository.RouteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RouteService {

    private final RouteRepository repository;

    public RouteService(RouteRepository repository) {
        this.repository = repository;
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
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
