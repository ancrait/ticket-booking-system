package com.sorokaandriy.auth_service.dto;

import com.sorokaandriy.auth_service.entity.Role;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        Boolean emailVerified,
        Instant createdAt
) {
}
