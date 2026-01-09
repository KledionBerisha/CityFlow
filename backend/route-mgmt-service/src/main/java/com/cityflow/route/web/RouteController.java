package com.cityflow.route.web;

import com.cityflow.route.dto.RouteRequest;
import com.cityflow.route.dto.RouteResponse;
import com.cityflow.route.service.RouteService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/routes")
public class RouteController {

    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse create(@Valid @RequestBody RouteRequest request) {
        return service.create(request);
    }

    @GetMapping
    public Page<RouteResponse> list(@PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public RouteResponse get(@PathVariable("id") UUID id) {
        return service.get(id);
    }
}
