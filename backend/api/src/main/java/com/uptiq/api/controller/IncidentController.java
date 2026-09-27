package com.uptiq.api.controller;

import com.uptiq.api.dto.IncidentResponse;
import com.uptiq.api.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> getIncidents() {

        return ResponseEntity.ok(
                incidentService.getAllIncidents()
        );
    }

    @GetMapping("/website/{websiteId}/incidents")
    public ResponseEntity<List<IncidentResponse>> getWebsiteIncidents(
            @PathVariable UUID websiteId
    ) {
        return ResponseEntity.ok(
                incidentService.getWebsiteIncidents(websiteId)
        );
    }
}