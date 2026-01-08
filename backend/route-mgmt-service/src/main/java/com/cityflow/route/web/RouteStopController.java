package com.cityflow.route.web;

import com.cityflow.route.dto.RouteStopRequest;
import com.cityflow.route.dto.RouteStopResponse;
import com.cityflow.route.service.RouteStopService;
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
@RequestMapping("/routes/{routeId}/stops")
public class RouteStopController {

    private final RouteStopService routeStopService;

    public RouteStopController(RouteStopService routeStopService) {
        this.routeStopService = routeStopService;
    }

    @GetMapping
    public List<RouteStopResponse> list(@PathVariable("routeId") UUID routeId) {
        return routeStopService.list(routeId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public List<RouteStopResponse> replace(@PathVariable("routeId") UUID routeId,
                                           @Valid @RequestBody List<RouteStopRequest> payload) {
        return routeStopService.replaceRouteStops(routeId, payload);
    }
}
