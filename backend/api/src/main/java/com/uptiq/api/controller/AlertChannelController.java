package com.uptiq.api.controller;

import com.uptiq.api.dto.AlertChannelRequest;
import com.uptiq.api.dto.AlertChannelResponse;
import com.uptiq.api.service.AlertChannelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class AlertChannelController {

    private final AlertChannelService alertChannelService;

    public AlertChannelController(AlertChannelService alertChannelService) {
        this.alertChannelService = alertChannelService;
    }

    @GetMapping("/alert-channels")
    public ResponseEntity<List<AlertChannelResponse>> getChannels() {
        return ResponseEntity.ok(alertChannelService.getChannels());
    }

    @PostMapping("/alert-channels")
    public ResponseEntity<AlertChannelResponse> createChannel(@Valid @RequestBody AlertChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertChannelService.createChannel(request));
    }

    @DeleteMapping("/alert-channels/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable UUID id) {
        alertChannelService.deleteChannel(id);
        return ResponseEntity.noContent().build();
    }
}