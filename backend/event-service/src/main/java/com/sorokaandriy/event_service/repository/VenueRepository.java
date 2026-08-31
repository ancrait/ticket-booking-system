package com.sorokaandriy.event_service.repository;

import com.sorokaandriy.event_service.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
}
