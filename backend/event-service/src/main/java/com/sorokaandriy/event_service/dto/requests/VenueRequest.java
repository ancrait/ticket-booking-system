package com.sorokaandriy.event_service.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record VenueRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "City is required")
        String city,
        @NotBlank(message = "Address is required")
        String address
) {
}
