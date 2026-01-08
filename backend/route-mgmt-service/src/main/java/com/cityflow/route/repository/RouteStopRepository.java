package com.cityflow.route.repository;

import com.cityflow.route.model.RouteStop;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {
    void deleteByRoute_Id(UUID routeId);
    List<RouteStop> findByRoute_IdOrderBySequenceOrderAsc(UUID routeId);
}
