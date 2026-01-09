package com.cityflow.route.web;

import com.cityflow.route.dto.StopRequest;
import com.cityflow.route.dto.StopResponse;
import com.cityflow.route.service.StopService;
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
@RequestMapping("/stops")
public class StopController {

    private final StopService stopService;

    public StopController(StopService stopService) {
        this.stopService = stopService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StopResponse create(@Valid @RequestBody StopRequest request) {
        return stopService.create(request);
    }

    @GetMapping
    public Page<StopResponse> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return stopService.list(pageable);
    }

    @GetMapping("/{id}")
    public StopResponse get(@PathVariable("id") UUID id) {
        return stopService.get(id);
    }
}
