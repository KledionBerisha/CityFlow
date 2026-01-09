package com.cityflow.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "alert_rules")
public class AlertRule {

    @Id
    private String id;
    
    private String name;
    private String description;
    private boolean enabled;
    
    // Rule conditions
    private String metricType;  // CONGESTION_LEVEL, CITY_SCORE, BUS_DELAY, etc.
    private String operator;    // GREATER_THAN, LESS_THAN, EQUALS
    private Double threshold;
    private String stringValue;  // For string comparisons (e.g., congestion level)
    
    // Notification settings
    private NotificationType notificationType;
    private NotificationPriority priority;
    private boolean sendWebSocket;
    private boolean sendEmail;

    public AlertRule() {
        this.enabled = true;
        this.sendWebSocket = true;
        this.sendEmail = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public boolean isSendWebSocket() {
        return sendWebSocket;
    }

    public void setSendWebSocket(boolean sendWebSocket) {
        this.sendWebSocket = sendWebSocket;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }
}
