package com.sorokaandriy.event_service.repository;

import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
    boolean existsByHallId(UUID hallId);

    Page<Seat> findAllByHall(Hall hall, Pageable pageable);
}
