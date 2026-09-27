package com.uptiq.api.dto;


import com.uptiq.entity.WebsiteSSLStatus;

import java.time.LocalDateTime;

public class WebsiteSSLStatusResponse {
    private String issuer;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer daysRemaining;
    private LocalDateTime lastCheckedAt;

    public WebsiteSSLStatusResponse(WebsiteSSLStatus sslStatus) {
        this.issuer = sslStatus.getIssuer();
        this.validFrom = sslStatus.getValidFrom();
        this.validUntil = sslStatus.getValidUntil();
        this.daysRemaining = sslStatus.getDaysRemaining();
        this.lastCheckedAt = sslStatus.getLastCheckedAt();
    }

    public String getIssuer() {
        return issuer;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public Integer getDaysRemaining() {
        return daysRemaining;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }
}