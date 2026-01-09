package com.cityflow.route.service;

import com.cityflow.route.dto.RouteStopRequest;
import com.cityflow.route.dto.RouteStopResponse;
import com.cityflow.route.model.Route;
import com.cityflow.route.model.RouteStop;
import com.cityflow.route.model.Stop;
import com.cityflow.route.repository.RouteRepository;
import com.cityflow.route.repository.RouteStopRepository;
import com.cityflow.route.repository.StopRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RouteStopService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteStopRepository routeStopRepository;

    public RouteStopService(RouteRepository routeRepository,
                            StopRepository stopRepository,
                            RouteStopRepository routeStopRepository) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @Transactional
    public List<RouteStopResponse> replaceRouteStops(UUID routeId, List<RouteStopRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one stop is required");
        }

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        validateUniqueness(requests);

        Set<UUID> stopIds = requests.stream().map(RouteStopRequest::getStopId).collect(Collectors.toSet());
        Map<UUID, Stop> stops = stopRepository.findAllById(stopIds).stream()
                .collect(Collectors.toMap(Stop::getId, s -> s));
        if (stops.size() != stopIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more stops not found");
        }

        // Replace atomically: delete then insert
        routeStopRepository.deleteByRoute_Id(routeId);

        List<RouteStop> entities = requests.stream()
                .map(req -> toEntity(route, stops.get(req.getStopId()), req))
                .toList();

        return routeStopRepository.saveAll(entities).stream()
                .sorted((a, b) -> Integer.compare(a.getSequenceOrder(), b.getSequenceOrder()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteStopResponse> list(UUID routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found");
        }
        return routeStopRepository.findByRoute_IdOrderBySequenceOrderAsc(routeId).stream()
                .map(this::toDto)
                .toList();
    }

    private void validateUniqueness(List<RouteStopRequest> requests) {
        Map<Integer, Integer> seqCounts = new HashMap<>();
        Map<UUID, Integer> stopCounts = new HashMap<>();
        for (RouteStopRequest r : requests) {
            if (r.getSequenceOrder() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sequenceOrder must be >= 1");
            }
            seqCounts.merge(r.getSequenceOrder(), 1, Integer::sum);
            stopCounts.merge(r.getStopId(), 1, Integer::sum);
        }
        boolean dupSeq = seqCounts.values().stream().anyMatch(c -> c > 1);
        boolean dupStop = stopCounts.values().stream().anyMatch(c -> c > 1);
        if (dupSeq) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate sequenceOrder in payload");
        }
        if (dupStop) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate stopId in payload");
        }
    }

    private RouteStop toEntity(Route route, Stop stop, RouteStopRequest req) {
        RouteStop rs = new RouteStop();
        rs.setRoute(route);
        rs.setStop(stop);
        rs.setSequenceOrder(req.getSequenceOrder());
        rs.setScheduledArrivalTime(req.getScheduledArrivalTime());
        rs.setScheduledDepartureTime(req.getScheduledDepartureTime());
        return rs;
    }

    private RouteStopResponse toDto(RouteStop rs) {
        RouteStopResponse dto = new RouteStopResponse();
        dto.setId(rs.getId());
        dto.setRouteId(rs.getRoute().getId());
        dto.setStopId(rs.getStop().getId());
        dto.setSequenceOrder(rs.getSequenceOrder());
        dto.setScheduledArrivalTime(rs.getScheduledArrivalTime());
        dto.setScheduledDepartureTime(rs.getScheduledDepartureTime());
        dto.setStopCode(rs.getStop().getCode());
        dto.setStopName(rs.getStop().getName());
        dto.setTerminal(rs.getStop().isTerminal());
        return dto;
    }
}
