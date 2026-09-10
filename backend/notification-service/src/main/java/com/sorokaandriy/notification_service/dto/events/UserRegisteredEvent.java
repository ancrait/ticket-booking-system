package com.sorokaandriy.notification_service.dto.events;

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
