package com.cityflow.traffic.web;

import com.cityflow.traffic.dto.TrafficReadingDto;
import com.cityflow.traffic.model.CongestionLevel;
import com.cityflow.traffic.service.TrafficReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/traffic")
public class TrafficReadingController {

    private static final Logger log = LoggerFactory.getLogger(TrafficReadingController.class);

    private final TrafficReadingService trafficReadingService;

    public TrafficReadingController(TrafficReadingService trafficReadingService) {
        this.trafficReadingService = trafficReadingService;
    }

    @GetMapping("/current")
    public Flux<TrafficReadingDto> getAllCurrentReadings() {
        log.debug("Fetching all current traffic readings");
        return trafficReadingService.getAllCurrentReadings()
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/current/{sensorId}")
    public Mono<TrafficReadingDto> getCurrentReading(@PathVariable String sensorId) {
        log.debug("Fetching current reading for sensor: {}", sensorId);
        return trafficReadingService.getCurrentReading(sensorId)
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/history/{sensorId}")
    public Flux<TrafficReadingDto> getReadingHistory(
            @PathVariable String sensorId,
            @RequestParam(defaultValue = "100") int limit) {
        log.debug("Fetching reading history for sensor: {} (limit: {})", sensorId, limit);
        return trafficReadingService.getReadingHistory(sensorId, Math.min(limit, 1000))
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/history/{sensorId}/range")
    public Flux<TrafficReadingDto> getReadingsByTimeRange(
            @PathVariable String sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        log.debug("Fetching readings for sensor {} from {} to {}", sensorId, start, end);
        return trafficReadingService.getReadingsByTimeRange(sensorId, start, end)
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/road-segment/{roadSegmentId}")
    public Flux<TrafficReadingDto> getReadingsByRoadSegment(
            @PathVariable String roadSegmentId,
            @RequestParam(defaultValue = "50") int limit) {
        log.debug("Fetching readings for road segment: {} (limit: {})", roadSegmentId, limit);
        return trafficReadingService.getReadingsByRoadSegment(roadSegmentId, Math.min(limit, 1000))
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/recent")
    public Flux<TrafficReadingDto> getRecentReadings(
            @RequestParam(defaultValue = "5") int minutes) {
        log.debug("Fetching readings from last {} minutes", minutes);
        return trafficReadingService.getRecentReadings(Duration.ofMinutes(minutes))
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/congestion/{level}")
    public Flux<TrafficReadingDto> getReadingsByCongestionLevel(
            @PathVariable CongestionLevel level) {
        log.debug("Fetching readings with congestion level: {}", level);
        return trafficReadingService.getReadingsByCongestionLevel(level)
                .map(TrafficReadingDto::fromEntity);
    }

    @GetMapping("/incidents")
    public Flux<TrafficReadingDto> getIncidentReadings() {
        log.debug("Fetching incident readings");
        return trafficReadingService.getIncidentReadings()
                .map(TrafficReadingDto::fromEntity);
    }
}
