package com.sorokaandriy.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.notification_service.dto.events.UserRegisteredEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthConsumerTest {

    @Mock
    private EmailNotificationService service;

    private ObjectMapper objectMapper;

    private AuthConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        consumer = new AuthConsumer(service, objectMapper);
    }

    private UserRegisteredEvent userRegisteredEvent() {
        return UserRegisteredEvent.builder()
                .userId("11111111-1111-1111-1111-111111111111")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .verificationToken("token-123")
                .registeredAt(Instant.parse("2026-10-04T10:00:00Z"))
                .build();
    }

    @Test
    void handleUserRegisterDeserializesPayloadAndDelegatesToService() throws Exception {
        UserRegisteredEvent event = userRegisteredEvent();

        consumer.handleUserRegister(objectMapper.writeValueAsString(event));

        verify(service).handleUserRegister(event);
    }

    @Test
    void handleUserRegisterWithMalformedPayloadDoesNotDelegateAndDoesNotThrow() {
        assertThatCode(() -> consumer.handleUserRegister("{not-json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(service);
    }
}
