package com.sorokaandriy.booking_service.dto.responses;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record HeldSeatInfo(
        UUID eventSeatId,
        Integer rowNumber,
        Integer seatNumber,
        String sector,
        BigDecimal price
) {
}
