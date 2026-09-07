package com.sorokaandriy.payment_service.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PaymentInitiateResponse(
        UUID paymentId,
        String clientSecret,
        BigDecimal amount,
        String currency
) {
}
