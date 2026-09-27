package com.uptiq.api.service;

import com.uptiq.api.dto.IncidentResponse;
import com.uptiq.api.exception.ResourceNotFoundException;
import com.uptiq.api.security.CurrentUserProvider;
import com.uptiq.repository.IncidentRepository;
import com.uptiq.repository.WebsiteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final WebsiteRepository websiteRepository;
    private final CurrentUserProvider currentUserProvider;

    public IncidentService(
            IncidentRepository incidentRepository,
            WebsiteRepository websiteRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.incidentRepository = incidentRepository;
        this.websiteRepository = websiteRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<IncidentResponse> getAllIncidents() {
        UUID userId = currentUserProvider.getCurrentUserId();
        Pageable pageable = PageRequest.of(0, 50, Sort.by("startedAt").descending());
        return incidentRepository
                .findAllByWebsite_User_Id(userId, pageable)
                .stream()
                .map(IncidentResponse::new)
                .toList();
    }

    public List<IncidentResponse> getWebsiteIncidents(UUID websiteId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        websiteRepository.findByIdAndUser_Id(websiteId, userId).orElseThrow(() -> new ResourceNotFoundException("Website not found"));
        Pageable pageable = PageRequest.of(0, 20, Sort.by("startedAt").descending());
        return incidentRepository
                .findAllByWebsite_Id(websiteId, pageable)
                .stream()
                .map(IncidentResponse::new)
                .toList();
    }
}