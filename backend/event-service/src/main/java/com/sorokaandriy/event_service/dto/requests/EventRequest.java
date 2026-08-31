package com.sorokaandriy.event_service.dto.requests;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record EventRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        String posterUrl,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        Instant startsAt,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        Instant endsAt,

        @NotNull(message = "Hall ID is required")
        UUID hallId
) {
}
