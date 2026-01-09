package com.cityflow.notification.repository;

import com.cityflow.notification.model.Notification;
import com.cityflow.notification.model.NotificationPriority;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

    Flux<Notification> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Flux<Notification> findByReadFalseOrderByTimestampDesc(Pageable pageable);

    Flux<Notification> findByPriorityOrderByTimestampDesc(NotificationPriority priority, Pageable pageable);

    Flux<Notification> findByTimestampAfterOrderByTimestampDesc(Instant since);
}
