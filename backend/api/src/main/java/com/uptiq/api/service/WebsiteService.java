package com.uptiq.api.service;

import com.uptiq.api.dto.CreateWebsiteRequest;
import com.uptiq.api.dto.WebsiteResponse;
import com.uptiq.api.dto.WebsiteTickResponse;
import com.uptiq.api.exception.ResourceNotFoundException;
import com.uptiq.api.security.CurrentUserProvider;
import com.uptiq.entity.User;
import com.uptiq.entity.Website;
import com.uptiq.entity.WebsiteTick;
import com.uptiq.repository.UserRepository;
import com.uptiq.repository.WebsiteRepository;
import com.uptiq.repository.WebsiteTickRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class WebsiteService {
    private final WebsiteRepository websiteRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final WebsiteTickRepository websiteTickRepository;
    private final JdbcTemplate jdbcTemplate;

    public WebsiteService(
            WebsiteRepository websiteRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider,
            WebsiteTickRepository websiteTickRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.websiteRepository = websiteRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.websiteTickRepository = websiteTickRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public WebsiteResponse createWebsite(CreateWebsiteRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Website website = new Website();
        website.setUser(user);
        website.setName(request.getName());
        website.setUrl(request.getUrl());
        website.setSlug(request.getSlug());
        website.setIsActive(true);
        Website savedWebsite = websiteRepository.save(website);
        return new WebsiteResponse(savedWebsite);
    }

    public List<WebsiteResponse> getWebsites() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return websiteRepository
                .findAllByUser_Id(userId)
                .stream()
                .map(WebsiteResponse::new)
                .toList();
    }

    public WebsiteResponse getWebsite(UUID websiteId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Website website = websiteRepository.findByIdAndUser_Id(websiteId, userId).orElseThrow(() -> new ResourceNotFoundException("Website not found"));
        return new WebsiteResponse(website);
    }

    @Transactional
    public void deleteWebsite(UUID websiteId) {

        UUID userId = currentUserProvider.getCurrentUserId();

        Website website = websiteRepository
                .findByIdAndUser_Id(websiteId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Website not found")
                );

        // These are not cascade-deleted from Website
        jdbcTemplate.update(
                "DELETE FROM website_ticks WHERE website_id = ?",
                websiteId
        );

        jdbcTemplate.update(
                "DELETE FROM incidents WHERE website_id = ?",
                websiteId
        );

        websiteRepository.delete(website);
    }

    public List<WebsiteTickResponse> getWebsiteTicks(UUID websiteId) {

        UUID userId = currentUserProvider.getCurrentUserId();

        websiteRepository
                .findByIdAndUser_Id(websiteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Website not found"));

        List<WebsiteTick> ticks = websiteTickRepository.findTop50ByWebsite_IdOrderByCheckedAtDesc(websiteId);

        Collections.reverse(ticks);

        return ticks.stream()
                .map(WebsiteTickResponse::new)
                .toList();
    }
}