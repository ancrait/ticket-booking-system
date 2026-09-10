package com.sorokaandriy.auth_service.kafka;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserRegisteredEvent(
        String userId,
        String email,
        String firstName,
        String lastName,
        String verificationToken,
        Instant registeredAt
) {
}
