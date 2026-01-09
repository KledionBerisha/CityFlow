package com.cityflow.notification.service;

import com.cityflow.notification.model.Notification;
import com.cityflow.notification.model.NotificationPriority;
import com.cityflow.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Mono<Notification> sendNotification(Notification notification) {
        return notificationRepository.save(notification)
                .doOnSuccess(saved -> {
                    log.info("Notification created: {} - {}", saved.getType(), saved.getTitle());
                    
                    // Send via WebSocket
                    try {
                        messagingTemplate.convertAndSend("/topic/notifications", saved);
                        saved.setSentViaWebSocket(true);
                        saved.setSent(true);
                        notificationRepository.save(saved).subscribe();
                        log.debug("Sent notification via WebSocket: {}", saved.getId());
                    } catch (Exception e) {
                        log.error("Failed to send WebSocket notification", e);
                    }
                });
    }

    public Flux<Notification> getRecentNotifications(int limit) {
        return notificationRepository.findByReadFalseOrderByTimestampDesc(PageRequest.of(0, limit));
    }

    public Flux<Notification> getNotificationsByPriority(NotificationPriority priority, int limit) {
        return notificationRepository.findByPriorityOrderByTimestampDesc(priority, PageRequest.of(0, limit));
    }

    public Flux<Notification> getNotificationsSince(Instant since) {
        return notificationRepository.findByTimestampAfterOrderByTimestampDesc(since);
    }

    public Mono<Notification> markAsRead(String id) {
        return notificationRepository.findById(id)
                .flatMap(notification -> {
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                });
    }

    public Mono<Void> deleteNotification(String id) {
        return notificationRepository.deleteById(id);
    }
}
