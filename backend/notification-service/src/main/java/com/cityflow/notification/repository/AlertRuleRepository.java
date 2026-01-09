package com.cityflow.notification.repository;

import com.cityflow.notification.model.AlertRule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AlertRuleRepository extends ReactiveMongoRepository<AlertRule, String> {

    Flux<AlertRule> findByEnabledTrue();

    Flux<AlertRule> findByMetricType(String metricType);
}
