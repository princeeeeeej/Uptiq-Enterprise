package com.uptiq;

import java.util.Map;
import java.util.UUID;

public record AlertChannelInfo(
        UUID id,
        String type,
        Map<String, String> config
) {
}