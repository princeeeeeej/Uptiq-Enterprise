package com.uptiq.api.dto;

import java.util.UUID;

public class SignupResponse {
    private UUID id;

    public SignupResponse(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
}