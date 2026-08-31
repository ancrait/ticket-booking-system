package com.sorokaandriy.event_service.dto.responses;

import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record EventSeatResponse(
        UUID id,
        UUID seatId,
        Integer rowNumber,
        Integer seatNumber,
        String sector,
        BigDecimal price,
        EventSeatStatus status
) {
}
