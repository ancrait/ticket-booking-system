package com.sorokaandriy.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
