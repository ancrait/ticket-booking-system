package com.sorokaandriy.payment_service.dto.events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BookingExpiredEvent(
        UUID bookingId,
        UUID userId,
        String email,
        UUID eventId,
        String reason
) {
}
