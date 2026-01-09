package com.cityflow.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private NotificationType type;
    private NotificationPriority priority;
    private String title;
    private String message;
    private String source;  // sensor-id, bus-id, road-segment-id
    private String sourceType;  // TRAFFIC, BUS, ANALYTICS
    
    @Indexed
    private Instant timestamp;
    private boolean read;
    private boolean sent;
    
    // Notification channels
    private boolean sentViaWebSocket;
    private boolean sentViaEmail;
    
    private String userId;  // Target user (if specific user)
    private String data;  // JSON additional data

    public Notification() {
        this.timestamp = Instant.now();
        this.read = false;
        this.sent = false;
        this.sentViaWebSocket = false;
        this.sentViaEmail = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public boolean isSentViaWebSocket() {
        return sentViaWebSocket;
    }

    public void setSentViaWebSocket(boolean sentViaWebSocket) {
        this.sentViaWebSocket = sentViaWebSocket;
    }

    public boolean isSentViaEmail() {
        return sentViaEmail;
    }

    public void setSentViaEmail(boolean sentViaEmail) {
        this.sentViaEmail = sentViaEmail;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
