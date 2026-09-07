package com.sorokaandriy.payment_service.dto.events;

import lombok.Builder;

import java.util.UUID;

@Builder
public record BookingCanceledEvent(
        UUID bookingId,
        UUID userId,
        UUID eventId,
        String reason
) {
}
