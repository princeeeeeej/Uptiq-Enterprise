package com.uptiq.api.dto;


import com.uptiq.entity.WebsiteTick;

import java.time.LocalDateTime;
import java.util.UUID;

public class WebsiteTickResponse {
    private UUID id;
    private String status;
    private Integer responseTimeMs;
    private Integer statusCode;
    private String errorMessage;
    private LocalDateTime checkedAt;
    private UUID regionId;
    private String regionCode;
    private String regionName;

    public WebsiteTickResponse(WebsiteTick tick) {
        this.id = tick.getId();
        this.status = tick.getStatus().name();
        this.responseTimeMs = tick.getResponseTimeMs();
        this.statusCode = tick.getStatusCode();
        this.errorMessage = tick.getErrorMessage();
        this.checkedAt = tick.getCheckedAt();

        this.regionId = tick.getRegion().getId();
        this.regionCode = tick.getRegion().getCode();
        this.regionName = tick.getRegion().getName();
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public UUID getRegionId() {
        return regionId;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getRegionName() {
        return regionName;
    }
}