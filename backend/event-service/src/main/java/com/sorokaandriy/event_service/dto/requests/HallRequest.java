package com.sorokaandriy.event_service.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HallRequest(
        @NotBlank(message = "Hall name is required")
        String name,

        @NotNull(message = "Rows count is required")
        @Positive(message = "Rows count must be greater than 0")
        Integer rowsCount,

        @NotNull(message = "Seats per row is required")
        @Positive(message = "Seats per row must be greater than 0")
        Integer seatsPerRow
) {
}
