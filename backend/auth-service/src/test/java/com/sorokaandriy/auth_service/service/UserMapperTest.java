package com.sorokaandriy.auth_service.service;

import com.sorokaandriy.auth_service.dto.RegisterRequest;
import com.sorokaandriy.auth_service.dto.UserResponse;
import com.sorokaandriy.auth_service.entity.Role;
import com.sorokaandriy.auth_service.entity.User;
import com.sorokaandriy.auth_service.kafka.UserRegisteredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void fromRegisterRequestToUser_mapsFieldsAndLowercasesEmail() {
        RegisterRequest request = new RegisterRequest(
                "John", "Doe", "John@Example.COM", "secret123", "+380501234567");

        User user = mapper.fromRegisterRequestToUser(request, "encoded-password");

        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getPhone()).isEqualTo("+380501234567");
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void fromUserToUserResponse_mapsAllFields() {
        User user = User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("john@example.com")
                .password("encoded-password")
                .firstName("John")
                .lastName("Doe")
                .phone("+380501234567")
                .role(Role.USER)
                .emailVerified(true)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();

        UserResponse response = mapper.fromUserToUserResponse(user);

        assertThat(response.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.phone()).isEqualTo("+380501234567");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.emailVerified()).isTrue();
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(response.id()).isNotEqualTo(user.getId());
    }

    @Test
    void fromUserToUserRegisteredEvent_mapsFields() {
        User user = User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserRegisteredEvent event = mapper.fromUserToUserRegisteredEvent(user, "verification-token");

        assertThat(event.userId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.firstName()).isEqualTo("John");
        assertThat(event.lastName()).isEqualTo("Doe");
        assertThat(event.verificationToken()).isEqualTo("verification-token");
        assertThat(event.registeredAt()).isNotNull();
    }
}
