package com.uptiq.repository;

import com.uptiq.entity.WebsiteStatusCurrent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebsiteStatusCurrentRepository extends JpaRepository<WebsiteStatusCurrent, UUID> {
}