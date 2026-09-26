package com.uptiq.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "website_ssl_status")
public class WebsiteSSLStatus {

    @Id
    @Column(name = "website_id")
    private UUID websiteId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "website_id")
    private Website website;

    private String issuer;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private Integer daysRemaining;

    @Column(nullable = false)
    private LocalDateTime lastCheckedAt;

    @PrePersist
    protected void onCreate() {
        if (lastCheckedAt == null) {
            lastCheckedAt = LocalDateTime.now();
        }
    }

    public UUID getWebsiteId() {
        return websiteId;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Integer getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Integer daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}