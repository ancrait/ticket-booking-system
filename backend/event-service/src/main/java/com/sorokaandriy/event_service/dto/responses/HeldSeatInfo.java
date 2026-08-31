package com.sorokaandriy.event_service.dto.responses;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record HeldSeatInfo(
        UUID seatId,
        BigDecimal price
) {
}
