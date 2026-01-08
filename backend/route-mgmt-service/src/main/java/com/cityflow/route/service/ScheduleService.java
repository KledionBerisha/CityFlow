package com.cityflow.route.service;

import com.cityflow.route.dto.ScheduleRequest;
import com.cityflow.route.dto.ScheduleResponse;
import com.cityflow.route.model.Route;
import com.cityflow.route.model.Schedule;
import com.cityflow.route.repository.RouteRepository;
import com.cityflow.route.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScheduleService {

    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;

    public ScheduleService(RouteRepository routeRepository, ScheduleRepository scheduleRepository) {
        this.routeRepository = routeRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public List<ScheduleResponse> replace(UUID routeId, List<ScheduleRequest> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one schedule entry is required");
        }

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        payload.forEach(this::validateRequest);

        scheduleRepository.deleteByRoute_Id(routeId);
        List<Schedule> toSave = payload.stream()
                .map(req -> toEntity(route, req))
                .toList();

        return scheduleRepository.saveAll(toSave).stream()
                .sorted((a, b) -> {
                    int c = Short.compare(a.getDayOfWeek(), b.getDayOfWeek());
                    if (c != 0) return c;
                    LocalTime t1 = a.getStartTime();
                    LocalTime t2 = b.getStartTime();
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return -1;
                    if (t2 == null) return 1;
                    return t1.compareTo(t2);
                })
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(UUID routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found");
        }
        return scheduleRepository.findByRoute_IdOrderByDayOfWeekAscStartTimeAsc(routeId).stream()
                .map(this::toDto)
                .toList();
    }

    private void validateRequest(ScheduleRequest req) {
        if (req.getDayOfWeek() == null || req.getDayOfWeek() < 0 || req.getDayOfWeek() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek must be between 0 and 6");
        }
        if (req.getFrequencyMin() != null && req.getFrequencyMin() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "frequencyMin must be positive");
        }
        LocalTime start = req.getStartTime();
        LocalTime end = req.getEndTime();
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }
        LocalDate from = req.getEffectiveFrom();
        LocalDate to = req.getEffectiveTo();
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "effectiveFrom must be before effectiveTo");
        }
    }

    private Schedule toEntity(Route route, ScheduleRequest req) {
        Schedule s = new Schedule();
        s.setRoute(route);
        s.setDayOfWeek(req.getDayOfWeek());
        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setFrequencyMin(req.getFrequencyMin());
        s.setTimezone(req.getTimezone() == null ? "UTC" : req.getTimezone().trim());
        s.setEffectiveFrom(req.getEffectiveFrom());
        s.setEffectiveTo(req.getEffectiveTo());
        return s;
    }

    private ScheduleResponse toDto(Schedule s) {
        ScheduleResponse dto = new ScheduleResponse();
        dto.setId(s.getId());
        dto.setRouteId(s.getRoute().getId());
        dto.setDayOfWeek(s.getDayOfWeek());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setFrequencyMin(s.getFrequencyMin());
        dto.setTimezone(s.getTimezone());
        dto.setEffectiveFrom(s.getEffectiveFrom());
        dto.setEffectiveTo(s.getEffectiveTo());
        return dto;
    }
}
