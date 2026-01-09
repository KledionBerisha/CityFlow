package com.cityflow.analytics.web;

import com.cityflow.analytics.model.CityWideMetrics;
import com.cityflow.analytics.model.RoadSegmentMetrics;
import com.cityflow.analytics.service.AnalyticsAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsAggregationService aggregationService;

    public AnalyticsController(AnalyticsAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/city")
    public Mono<CityWideMetrics> getCityWideMetrics() {
        log.debug("Fetching city-wide metrics");
        return aggregationService.getCityMetrics()
                .switchIfEmpty(Mono.just(new CityWideMetrics()));
    }

    @GetMapping("/segments")
    public Flux<RoadSegmentMetrics> getAllRoadSegmentMetrics() {
        log.debug("Fetching all road segment metrics");
        return aggregationService.getAllRoadSegmentMetrics()
                .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/segments/{roadSegmentId}")
    public Mono<RoadSegmentMetrics> getRoadSegmentMetrics(@PathVariable String roadSegmentId) {
        log.debug("Fetching metrics for road segment: {}", roadSegmentId);
        return aggregationService.getRoadSegmentMetrics(roadSegmentId);
    }
}
