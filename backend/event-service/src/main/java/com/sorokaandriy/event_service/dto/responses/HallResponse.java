package com.sorokaandriy.event_service.dto.responses;

import lombok.Builder;

import java.util.UUID;

@Builder
public record HallResponse(
        UUID id,
        String name,
        Integer rowsCount,
        Integer seatsPerRow,
        UUID venueId
) {
}
