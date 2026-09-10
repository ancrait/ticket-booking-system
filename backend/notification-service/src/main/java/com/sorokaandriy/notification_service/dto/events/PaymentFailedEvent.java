package com.sorokaandriy.notification_service.dto.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentFailedEvent(
        UUID bookingId,
        UUID userId,
        String email,
        UUID paymentId,
        String reason,
        Instant failedAt
) {
}
