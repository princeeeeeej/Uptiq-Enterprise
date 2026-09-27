package com.uptiq.api.controller;

import com.uptiq.api.dto.CreateWebsiteRequest;
import com.uptiq.api.dto.WebsiteResponse;
import com.uptiq.api.dto.WebsiteTickResponse;
import com.uptiq.api.service.WebsiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class WebsiteController {
    private final WebsiteService websiteService;

    public WebsiteController(WebsiteService websiteService) {
        this.websiteService = websiteService;
    }

    @PostMapping("/website")
    public ResponseEntity<WebsiteResponse> createWebsite(@Valid @RequestBody CreateWebsiteRequest request) {
        WebsiteResponse response = websiteService.createWebsite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/websites")
    public ResponseEntity<List<WebsiteResponse>> getWebsites() {
        List<WebsiteResponse> websites = websiteService.getWebsites();
        return ResponseEntity.ok(websites);
    }

    @GetMapping("/website/{websiteId}")
    public ResponseEntity<WebsiteResponse> getWebsite(@PathVariable UUID websiteId) {
        return ResponseEntity.ok(websiteService.getWebsite(websiteId));
    }

    @DeleteMapping("/website/{websiteId}")
    public ResponseEntity<Void> deleteWebsite(@PathVariable UUID websiteId) {
        websiteService.deleteWebsite(websiteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/website/{websiteId}/ticks")
    public ResponseEntity<List<WebsiteTickResponse>> getWebsiteTicks(@PathVariable UUID websiteId) {
        List<WebsiteTickResponse> ticks = websiteService.getWebsiteTicks(websiteId);
        return ResponseEntity.ok(ticks);
    }
}