package com.uptiq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "website_status_current")
public class WebsiteStatusCurrent {

    @Id
    @Column(name = "website_id")
    private UUID websiteId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "website_id")
    private Website website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebsiteStatus currentStatus = WebsiteStatus.UNKNOWN;

    @Column(nullable = false)
    private int consecutiveFails = 0;

    private LocalDateTime lastCheckedAt;

    private Integer lastResponseTimeMs;

    @Column(nullable = false)
    private int consensusRegions = 0;

    public UUID getWebsiteId() {
        return websiteId;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    public WebsiteStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(WebsiteStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public int getConsecutiveFails() {
        return consecutiveFails;
    }

    public void setConsecutiveFails(int consecutiveFails) {
        this.consecutiveFails = consecutiveFails;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Integer getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public void setLastResponseTimeMs(Integer lastResponseTimeMs) {
        this.lastResponseTimeMs = lastResponseTimeMs;
    }

    public int getConsensusRegions() {
        return consensusRegions;
    }

    public void setConsensusRegions(int consensusRegions) {
        this.consensusRegions = consensusRegions;
    }
}