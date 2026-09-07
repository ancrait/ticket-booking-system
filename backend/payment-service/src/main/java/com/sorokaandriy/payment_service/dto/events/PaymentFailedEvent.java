package com.sorokaandriy.payment_service.dto.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentFailedEvent(
        UUID bookingId,
        UUID userId,
        UUID paymentId,
        String reason,
        Instant failedAt
) {
}
