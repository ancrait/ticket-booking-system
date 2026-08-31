package com.sorokaandriy.event_service.dto.responses;

import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record EventResponse(
        UUID id,
        String title,
        String description,
        String posterUrl,
        Instant startsAt,
        Instant endsAt,
        EventStatus status,
        UUID organizerId,
        UUID hallId,
        Instant createdAt
) {
}
