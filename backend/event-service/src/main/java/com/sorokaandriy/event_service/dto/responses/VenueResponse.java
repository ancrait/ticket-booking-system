package com.sorokaandriy.event_service.dto.responses;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record VenueResponse(
    UUID id,
    String name,
    String city,
    String address,
    Instant createdAt
) {
}
