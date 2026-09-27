package com.uptiq.api.dto;

import com.uptiq.entity.AlertChannel;
import com.uptiq.entity.AlertType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AlertChannelResponse {

    private UUID id;
    private AlertType type;
    private Map<String, String> config;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public AlertChannelResponse(AlertChannel channel) {
        this.id = channel.getId();
        this.type = channel.getType();
        this.config = channel.getConfig();
        this.isActive = channel.getIsActive();
        this.createdAt = channel.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public AlertType getType() {
        return type;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}