package com.sorokaandriy.auth_service.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ErrorValidationResponse(
        int status,
        String error,
        Map<String, String> errors,
        Instant timestamp
) {
}
