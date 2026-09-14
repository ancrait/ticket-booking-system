package com.sorokaandriy.event_service.repository;

import com.sorokaandriy.event_service.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    Page<Venue> findByCityIgnoreCase(String city, Pageable pageable);
}
