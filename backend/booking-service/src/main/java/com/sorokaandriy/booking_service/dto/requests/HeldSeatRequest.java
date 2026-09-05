package com.sorokaandriy.booking_service.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record HeldSeatRequest(
        @NotEmpty(message = "Seat IDs list cannot be empty")
        List<UUID> seatIds
) {
}
