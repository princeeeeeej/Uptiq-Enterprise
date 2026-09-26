package com.uptiq.repository;

import com.uptiq.entity.WebsiteTick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebsiteTickRepository extends JpaRepository<WebsiteTick, UUID> {

    List<WebsiteTick> findTop50ByWebsite_IdOrderByCheckedAtDesc(UUID websiteId);
}