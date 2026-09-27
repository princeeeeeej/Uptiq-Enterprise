package com.uptiq.api.service;

import com.uptiq.api.dto.AlertChannelRequest;
import com.uptiq.api.dto.AlertChannelResponse;
import com.uptiq.api.exception.ResourceNotFoundException;
import com.uptiq.api.security.CurrentUserProvider;
import com.uptiq.entity.AlertChannel;
import com.uptiq.entity.User;
import com.uptiq.repository.AlertChannelRepository;
import com.uptiq.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AlertChannelService {

    private final AlertChannelRepository alertChannelRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public AlertChannelService(
            AlertChannelRepository alertChannelRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.alertChannelRepository = alertChannelRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public AlertChannelResponse createChannel(AlertChannelRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AlertChannel channel = new AlertChannel();
        channel.setUser(user);
        channel.setType(request.getType());
        channel.setConfig(request.getConfig());
        channel.setIsActive(true);
        AlertChannel savedChannel = alertChannelRepository.save(channel);
        return new AlertChannelResponse(savedChannel);
    }

    public List<AlertChannelResponse> getChannels() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return alertChannelRepository
                .findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AlertChannelResponse::new)
                .toList();
    }

    public void deleteChannel(UUID channelId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        AlertChannel channel = alertChannelRepository.findById(channelId).orElseThrow(() -> new ResourceNotFoundException("Alert channel not found"));
        if (!channel.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Alert channel not found");
        }
        alertChannelRepository.delete(channel);
    }
}