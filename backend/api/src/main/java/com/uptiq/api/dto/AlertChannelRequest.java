package com.uptiq.api.dto;


import com.uptiq.entity.AlertType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class AlertChannelRequest {

    @NotNull
    private AlertType type;

    @NotNull
    private Map<String, String> config;

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}