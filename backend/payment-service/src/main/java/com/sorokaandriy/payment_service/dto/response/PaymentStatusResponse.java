package com.sorokaandriy.payment_service.dto.response;

import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentStatusResponse(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerPaymentId,
        Instant updatedAt
) {
}
