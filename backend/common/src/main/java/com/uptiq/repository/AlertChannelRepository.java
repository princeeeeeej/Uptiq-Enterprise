package com.uptiq.repository;

import com.uptiq.entity.AlertChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertChannelRepository extends JpaRepository<AlertChannel, UUID> {

    List<AlertChannel> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

}