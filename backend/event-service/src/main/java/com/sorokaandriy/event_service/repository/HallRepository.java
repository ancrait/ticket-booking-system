package com.sorokaandriy.event_service.repository;

import com.sorokaandriy.event_service.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HallRepository extends JpaRepository<Hall, UUID> {
    List<Hall> findByVenueId(UUID venueId);
}
