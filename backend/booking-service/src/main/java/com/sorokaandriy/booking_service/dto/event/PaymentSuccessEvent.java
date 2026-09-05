package com.sorokaandriy.booking_service.dto.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentSuccessEvent(
        UUID bookingId,
        UUID userId,
        String paymentId,
        BigDecimal amount,
        Instant paidAt
) {
}
