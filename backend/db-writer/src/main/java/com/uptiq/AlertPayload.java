package com.uptiq;

import com.uptiq.entity.AlertEventType;

import java.util.UUID;

public record AlertPayload(
        UUID websiteId,
        String websiteName,
        String websiteUrl,
        AlertEventType eventType,
        String reason,
        Integer daysRemaining
) {
}