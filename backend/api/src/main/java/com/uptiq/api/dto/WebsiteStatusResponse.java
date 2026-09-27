package com.uptiq.api.dto;


import com.uptiq.entity.WebsiteStatusCurrent;

import java.time.LocalDateTime;

public class WebsiteStatusResponse {
    private String status;
    private int consecutiveFails;
    private LocalDateTime lastCheckedAt;
    private Integer lastResponseTimeMs;
    private int consensusRegions;

    public WebsiteStatusResponse(WebsiteStatusCurrent status) {
        this.status = status.getCurrentStatus().name();
        this.consecutiveFails = status.getConsecutiveFails();
        this.lastCheckedAt = status.getLastCheckedAt();
        this.lastResponseTimeMs = status.getLastResponseTimeMs();
        this.consensusRegions = status.getConsensusRegions();
    }

    public String getStatus() {
        return status;
    }

    public int getConsecutiveFails() {
        return consecutiveFails;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Integer getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public int getConsensusRegions() {
        return consensusRegions;
    }
}