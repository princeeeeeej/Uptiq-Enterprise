package com.uptiq.repository;

import com.uptiq.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {

    Optional<AlertEvent> findFirstByWebsite_IdAndAlertChannel_IdOrderByCreatedAtDesc(
            UUID websiteId,
            UUID alertChannelId
    );
}