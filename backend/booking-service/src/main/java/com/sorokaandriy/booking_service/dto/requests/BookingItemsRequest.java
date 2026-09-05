package com.sorokaandriy.booking_service.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record BookingItemsRequest(
        @NotNull(message = "Event seat ID is required")
        UUID eventSeatId
) {
}
