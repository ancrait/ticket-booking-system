package com.sorokaandriy.payment_service.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        String email,
        UUID eventId,
        String eventTitle,
        String status,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant expiresAt
) {
}
