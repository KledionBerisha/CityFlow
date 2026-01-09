package com.cityflow.notification.web;

import com.cityflow.notification.model.Notification;
import com.cityflow.notification.model.NotificationPriority;
import com.cityflow.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/recent")
    public Flux<Notification> getRecentNotifications(
            @RequestParam(defaultValue = "50") int limit) {
        log.debug("Fetching recent notifications (limit: {})", limit);
        return notificationService.getRecentNotifications(Math.min(limit, 200));
    }

    @GetMapping("/priority/{priority}")
    public Flux<Notification> getNotificationsByPriority(
            @PathVariable NotificationPriority priority,
            @RequestParam(defaultValue = "50") int limit) {
        log.debug("Fetching notifications by priority: {}", priority);
        return notificationService.getNotificationsByPriority(priority, Math.min(limit, 200));
    }

    @GetMapping("/since")
    public Flux<Notification> getNotificationsSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        log.debug("Fetching notifications since: {}", since);
        return notificationService.getNotificationsSince(since);
    }

    @PatchMapping("/{id}/read")
    public Mono<Notification> markAsRead(@PathVariable String id) {
        log.debug("Marking notification as read: {}", id);
        return notificationService.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteNotification(@PathVariable String id) {
        log.debug("Deleting notification: {}", id);
        return notificationService.deleteNotification(id);
    }
}
