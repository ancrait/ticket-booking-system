package com.sorokaandriy.notification_service.dto.events;

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
