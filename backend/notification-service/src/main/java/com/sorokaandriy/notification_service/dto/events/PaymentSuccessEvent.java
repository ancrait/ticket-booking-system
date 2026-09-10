package com.sorokaandriy.notification_service.dto.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentSuccessEvent(
        UUID bookingId,
        UUID userId,
        String email,
        String paymentId,
        BigDecimal amount,
        Instant paidAt
) {
}
