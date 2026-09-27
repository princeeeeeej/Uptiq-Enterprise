package com.uptiq.api.dto;


import com.uptiq.entity.Website;

import java.time.LocalDateTime;
import java.util.UUID;

public class WebsiteResponse {
    private UUID id;
    private String name;
    private String url;
    private String slug;
    private Boolean isActive;
    private WebsiteStatusResponse status;
    private WebsiteSSLStatusResponse sslStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WebsiteResponse(Website website) {
        this.id = website.getId();
        this.name = website.getName();
        this.url = website.getUrl();
        this.slug = website.getSlug();
        this.isActive = website.getIsActive();
        this.createdAt = website.getCreatedAt();
        this.updatedAt = website.getUpdatedAt();
        if (website.getStatus() != null) {
            this.status = new WebsiteStatusResponse(website.getStatus());
        }
        if (website.getSslStatus() != null) {
            this.sslStatus = new WebsiteSSLStatusResponse(website.getSslStatus());
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getSlug() {
        return slug;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public WebsiteStatusResponse getStatus() {
        return status;
    }

    public WebsiteSSLStatusResponse getSslStatus() {
        return sslStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}