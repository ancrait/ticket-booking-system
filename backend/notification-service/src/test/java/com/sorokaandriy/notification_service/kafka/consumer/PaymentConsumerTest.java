package com.sorokaandriy.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAYMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private EmailNotificationService service;

    private ObjectMapper objectMapper;

    private PaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        consumer = new PaymentConsumer(service, objectMapper);
    }

    private PaymentSuccessEvent paymentSuccessEvent() {
        return PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .paymentId("pay_123")
                .amount(new BigDecimal("150.00"))
                .paidAt(Instant.parse("2026-10-04T12:00:00Z"))
                .build();
    }

    private PaymentFailedEvent paymentFailedEvent() {
        return PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .paymentId(PAYMENT_ID)
                .reason("card declined")
                .failedAt(Instant.parse("2026-10-04T12:30:00Z"))
                .build();
    }

    @Test
    void handlePaymentSuccessEventDeserializesPayloadAndDelegatesToService() throws Exception {
        PaymentSuccessEvent event = paymentSuccessEvent();

        consumer.handlePaymentSuccessEvent(objectMapper.writeValueAsString(event));

        verify(service).handlePaymentSuccess(event);
    }

    @Test
    void handlePaymentSuccessEventWithMalformedPayloadDoesNotDelegateAndDoesNotThrow() {
        assertThatCode(() -> consumer.handlePaymentSuccessEvent("{not-json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(service);
    }

    @Test
    void handlePaymentFailedEventDeserializesPayloadAndDelegatesToService() throws Exception {
        PaymentFailedEvent event = paymentFailedEvent();

        consumer.handlePaymentFailedEvent(objectMapper.writeValueAsString(event));

        verify(service).handlePaymentFailed(event);
    }

    @Test
    void handlePaymentFailedEventWithMalformedPayloadDoesNotDelegateAndDoesNotThrow() {
        assertThatCode(() -> consumer.handlePaymentFailedEvent("{not-json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(service);
    }
}
