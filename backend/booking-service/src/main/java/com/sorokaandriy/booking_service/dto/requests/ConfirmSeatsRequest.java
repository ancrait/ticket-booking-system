package com.sorokaandriy.booking_service.dto.requests;


import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ConfirmSeatsRequest(
        @NotEmpty(message = "Seat IDs list cannot be empty")
        List<UUID> seatIds
) {
}
