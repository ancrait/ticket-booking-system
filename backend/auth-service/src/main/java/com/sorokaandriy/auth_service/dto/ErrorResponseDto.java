package com.sorokaandriy.auth_service.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ErrorResponseDto(
        int status,
        String error,
        String message,
        Instant timestamp
) {
}
