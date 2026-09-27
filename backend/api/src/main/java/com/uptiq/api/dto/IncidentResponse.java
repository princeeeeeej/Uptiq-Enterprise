package com.uptiq.api.dto;

import com.uptiq.entity.Incident;

import java.time.LocalDateTime;
import java.util.UUID;

public class IncidentResponse {

    private UUID id;

    private UUID websiteId;
    private String websiteName;
    private String websiteUrl;

    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    private String reason;
    private Integer durationSeconds;

    public IncidentResponse(Incident incident) {
        this.id = incident.getId();

        this.websiteId = incident.getWebsite().getId();
        this.websiteName = incident.getWebsite().getName();
        this.websiteUrl = incident.getWebsite().getUrl();

        this.startedAt = incident.getStartedAt();
        this.resolvedAt = incident.getResolvedAt();
        this.reason = incident.getReason();
        this.durationSeconds = incident.getDurationSeconds();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWebsiteId() {
        return websiteId;
    }

    public String getWebsiteName() {
        return websiteName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getReason() {
        return reason;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }
}