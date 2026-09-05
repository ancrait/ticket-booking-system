package com.sorokaandriy.booking_service.repository;

import com.sorokaandriy.booking_service.dto.responses.BookingResponse;
import com.sorokaandriy.booking_service.entity.Booking;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findAllByUserId(UUID userId, Pageable pageable);

    List<Booking> findAllByStatusAndExpiresAtBefore(BookingStatus bookingStatus, Instant now);
}
