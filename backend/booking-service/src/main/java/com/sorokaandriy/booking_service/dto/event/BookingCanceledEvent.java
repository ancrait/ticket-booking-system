package com.sorokaandriy.booking_service.dto.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BookingCanceledEvent(
        UUID bookingId,
        UUID userId,
        String email,
        UUID eventId,
        String reason
) {
}
