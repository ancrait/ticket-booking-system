package com.sorokaandriy.booking_service.dto.requests;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReleaseSeatsRequest(
        @NotEmpty List<UUID> seatIds
) {
}
