package com.uptiq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "alert_events",
        indexes = {
                @Index(name = "idx_alert_event_website_channel", columnList = "website_id, alert_channel_id"),
                @Index(name = "idx_alert_event_created_at", columnList = "created_at")
        }
)
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private Website website;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_channel_id", nullable = false)
    private AlertChannel alertChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertEventType eventType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public Website getWebsite() { return website; }
    public void setWebsite(Website website) { this.website = website; }

    public AlertChannel getAlertChannel() { return alertChannel; }
    public void setAlertChannel(AlertChannel alertChannel) { this.alertChannel = alertChannel; }

    public AlertEventType getEventType() { return eventType; }
    public void setEventType(AlertEventType eventType) { this.eventType = eventType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}