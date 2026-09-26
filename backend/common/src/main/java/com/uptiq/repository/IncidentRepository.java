package com.uptiq.repository;


import com.uptiq.entity.Incident;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByWebsite_User_Id(UUID userId, Pageable pageable);

    List<Incident> findAllByWebsite_Id(UUID websiteId, Pageable pageable);
}