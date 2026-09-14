package com.sorokaandriy.event_service.repository;

import com.sorokaandriy.event_service.entity.EventSeat;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    Page<EventSeat> findByEventId(UUID eventId, Pageable pageable);

    Page<EventSeat> findEventSeatByEventIdAndStatus(UUID eventId, EventSeatStatus status, Pageable pageable);

    boolean existsByEventId(UUID eventId);

    List<EventSeat> findAllByEventIdAndIdInAndStatus(
            UUID eventId, List<UUID> seatIds, EventSeatStatus status);

    List<EventSeat> findAllByEventIdAndIdIn(UUID eventId, List<UUID> seatIds);
}
