package com.sorokaandriy.booking_service.dto.responses;

import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record BookingResponse(
        UUID id,
        UUID userId,
        String email,
        UUID eventId,
        String eventTitle,
        BookingStatus status,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant expiresAt
) {
}
