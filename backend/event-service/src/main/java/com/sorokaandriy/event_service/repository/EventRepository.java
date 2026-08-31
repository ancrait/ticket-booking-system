package com.sorokaandriy.event_service.repository;

import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByOrganizerId(UUID organizerId);

    @Query("""
    SELECT e FROM Event e
    JOIN e.hall h
    JOIN h.venue v
    WHERE e.status = :status
      AND (:city IS NULL OR v.city = :city)
      AND (:date IS NULL OR CAST(e.startsAt AS LocalDate) = :date)
    """)
    Page<Event> findPublishedEvents(
            @Param("status") EventStatus status,
            @Param("city") String city,
            @Param("date") LocalDate date,
            Pageable pageable
    );
}
