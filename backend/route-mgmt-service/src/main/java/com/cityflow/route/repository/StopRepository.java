package com.cityflow.route.repository;

import com.cityflow.route.model.Stop;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, UUID> {
    boolean existsByCode(String code);
    Optional<Stop> findByCode(String code);
}
