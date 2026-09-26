package com.uptiq.repository;

import com.uptiq.entity.Website;
import com.uptiq.repository.projection.WebsiteCheckTarget;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebsiteRepository extends JpaRepository<Website, UUID> {

    List<Website> findAllByUser_Id(UUID userId);

    Optional<Website> findByIdAndUser_Id(UUID websiteId, UUID userId);

    Slice<WebsiteCheckTarget> findByIsActiveTrue(Pageable pageable);
}