package com.cityflow.route.repository;

import com.cityflow.route.model.Route;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, UUID> {
    boolean existsByCode(String code);
    Optional<Route> findByCode(String code);
}
