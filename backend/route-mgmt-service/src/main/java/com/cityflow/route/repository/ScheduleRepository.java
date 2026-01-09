package com.cityflow.route.repository;

import com.cityflow.route.model.Schedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    List<Schedule> findByRoute_IdOrderByDayOfWeekAscStartTimeAsc(UUID routeId);
    void deleteByRoute_Id(UUID routeId);
}
