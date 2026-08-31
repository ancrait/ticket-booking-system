package com.sorokaandriy.event_service.dto.responses;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SeatResponse(
        UUID id,
        Integer rowNumber,
        Integer seatNumber,
        String sector,
        UUID hallId
) {
}
