package com.cityflow.route.service;

import com.cityflow.route.dto.StopRequest;
import com.cityflow.route.dto.StopResponse;
import com.cityflow.route.model.Stop;
import com.cityflow.route.repository.StopRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StopService {

    private final StopRepository repository;

    public StopService(StopRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StopResponse create(StopRequest request) {
        String code = safeTrim(request.getCode());
        String name = safeTrim(request.getName());

        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stop name is required");
        }

        if (!code.isEmpty() && repository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stop code already exists");
        }

        Stop stop = new Stop();
        stop.setCode(code.isEmpty() ? null : code);
        stop.setName(name);
        stop.setLatitude(normalizeCoord(request.getLatitude(), "latitude", -90, 90));
        stop.setLongitude(normalizeCoord(request.getLongitude(), "longitude", -180, 180));
        stop.setTerminal(request.isTerminal());
        stop.setActive(request.isActive());

        Stop saved = repository.save(stop);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<StopResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public StopResponse get(UUID id) {
        Stop stop = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found"));
        return toDto(stop);
    }

    private StopResponse toDto(Stop stop) {
        StopResponse dto = new StopResponse();
        dto.setId(stop.getId());
        dto.setCode(stop.getCode());
        dto.setName(stop.getName());
        dto.setLatitude(stop.getLatitude());
        dto.setLongitude(stop.getLongitude());
        dto.setTerminal(stop.isTerminal());
        dto.setActive(stop.isActive());
        return dto;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal normalizeCoord(BigDecimal value, String field, int min, int max) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        if (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " out of range");
        }
        return value;
    }
}
