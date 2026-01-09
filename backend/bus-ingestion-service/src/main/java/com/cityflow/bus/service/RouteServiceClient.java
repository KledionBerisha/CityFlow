package com.cityflow.bus.service;

import com.cityflow.bus.dto.RouteStopWithCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Client for fetching route information from Route Management Service
 */
@Service
public class RouteServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(RouteServiceClient.class);
    
    private final WebClient webClient;
    private final String routeServiceUrl;
    
    public RouteServiceClient(@Value("${app.route-service.url:http://localhost:8081}") String routeServiceUrl) {
        this.routeServiceUrl = routeServiceUrl;
        this.webClient = WebClient.builder()
                .baseUrl(routeServiceUrl)
                .build();
    }
    
    /**
     * Fetch route stops with coordinates for a given route
     */
    public Mono<List<RouteStopWithCoordinates>> getRouteStops(UUID routeId) {
        log.debug("Fetching route stops for route: {}", routeId);
        
        return webClient.get()
                .uri("/routes/{routeId}/stops", routeId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RouteStopResponse>>() {})
                .flatMap(routeStops -> {
                    if (routeStops.isEmpty()) {
                        log.warn("No stops found for route: {}", routeId);
                        return Mono.just(List.<RouteStopWithCoordinates>of());
                    }
                    
                    // Fetch stop details to get coordinates
                    List<Mono<RouteStopWithCoordinates>> enrichedStops = routeStops.stream()
                            .map(rs -> fetchStopDetails(rs.getStopId())
                                    .map(stop -> new RouteStopWithCoordinates(
                                            rs.getStopId(),
                                            rs.getSequenceOrder(),
                                            rs.getStopName(),
                                            rs.getStopCode(),
                                            rs.isTerminal(),
                                            stop.getLatitude().doubleValue(),
                                            stop.getLongitude().doubleValue()
                                    ))
                                    .onErrorResume(e -> {
                                        log.error("Failed to fetch stop details for stop: {}", rs.getStopId(), e);
                                        return Mono.empty();
                                    }))
                            .collect(Collectors.toList());
                    
                    return Flux.fromIterable(enrichedStops)
                            .flatMap(mono -> mono)
                            .collectList()
                            .map(stops -> stops.stream()
                                    .sorted((a, b) -> Integer.compare(a.getSequenceOrder(), b.getSequenceOrder()))
                                    .collect(Collectors.toList()));
                })
                .onErrorMap(e -> {
                    log.error("Failed to fetch route stops for route: {}", routeId, e);
                    return new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Route service unavailable: " + e.getMessage(),
                            e
                    );
                });
    }
    
    /**
     * Fetch stop details including coordinates
     */
    private Mono<StopResponse> fetchStopDetails(UUID stopId) {
        return webClient.get()
                .uri("/stops/{stopId}", stopId)
                .retrieve()
                .bodyToMono(StopResponse.class)
                .onErrorMap(e -> {
                    log.error("Failed to fetch stop details for stop: {}", stopId, e);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Stop not found: " + stopId,
                            e
                    );
                });
    }
    
    // DTOs for route service responses
    private static class RouteStopResponse {
        private UUID stopId;
        private int sequenceOrder;
        private String stopName;
        private String stopCode;
        private boolean terminal;
        
        public UUID getStopId() { return stopId; }
        public void setStopId(UUID stopId) { this.stopId = stopId; }
        public int getSequenceOrder() { return sequenceOrder; }
        public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
        public String getStopName() { return stopName; }
        public void setStopName(String stopName) { this.stopName = stopName; }
        public String getStopCode() { return stopCode; }
        public void setStopCode(String stopCode) { this.stopCode = stopCode; }
        public boolean isTerminal() { return terminal; }
        public void setTerminal(boolean terminal) { this.terminal = terminal; }
    }
    
    private static class StopResponse {
        private BigDecimal latitude;
        private BigDecimal longitude;
        
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    }
}

