package com.cityflow.notification.service;

import com.cityflow.notification.dto.TrafficReadingEvent;
import com.cityflow.notification.model.*;
import com.cityflow.notification.repository.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AlertProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AlertProcessingService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final NotificationService notificationService;

    public AlertProcessingService(AlertRuleRepository alertRuleRepository, NotificationService notificationService) {
        this.alertRuleRepository = alertRuleRepository;
        this.notificationService = notificationService;
    }

    public void processTrafficEvent(TrafficReadingEvent event) {
        // Check for incidents
        if (Boolean.TRUE.equals(event.getIncidentDetected())) {
            createIncidentNotification(event);
        }

        // Check for severe congestion
        if ("SEVERE".equals(event.getCongestionLevel()) || "HEAVY".equals(event.getCongestionLevel())) {
            createCongestionNotification(event);
        }
    }

    private void createIncidentNotification(TrafficReadingEvent event) {
        Notification notification = new Notification();
        notification.setType(NotificationType.TRAFFIC_INCIDENT);
        notification.setPriority(NotificationPriority.CRITICAL);
        notification.setTitle("Traffic Incident Detected");
        notification.setMessage(String.format("Incident detected at sensor %s on road segment %s", 
                event.getSensorCode(), event.getRoadSegmentId()));
        notification.setSource(event.getSensorId());
        notification.setSourceType("TRAFFIC");
        
        notificationService.sendNotification(notification).subscribe(
                saved -> log.info("Created incident notification: {}", saved.getId()),
                error -> log.error("Failed to create incident notification", error)
        );
    }

    private void createCongestionNotification(TrafficReadingEvent event) {
        Notification notification = new Notification();
        notification.setType(NotificationType.TRAFFIC_CONGESTION);
        notification.setPriority("SEVERE".equals(event.getCongestionLevel()) ? 
                NotificationPriority.CRITICAL : NotificationPriority.WARNING);
        notification.setTitle(event.getCongestionLevel() + " Congestion");
        notification.setMessage(String.format("%s congestion at %s (Speed: %.1f km/h, Vehicles: %d)", 
                event.getCongestionLevel(), event.getSensorCode(), 
                event.getAverageSpeed(), event.getVehicleCount()));
        notification.setSource(event.getSensorId());
        notification.setSourceType("TRAFFIC");
        
        notificationService.sendNotification(notification).subscribe(
                saved -> log.debug("Created congestion notification: {}", saved.getId()),
                error -> log.error("Failed to create congestion notification", error)
        );
    }
}
