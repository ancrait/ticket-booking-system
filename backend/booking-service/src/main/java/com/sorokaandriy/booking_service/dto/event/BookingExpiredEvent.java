package com.sorokaandriy.booking_service.dto.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BookingExpiredEvent(
        UUID bookingId,
        UUID userId,
        UUID eventId,
        String reason
) {
}
