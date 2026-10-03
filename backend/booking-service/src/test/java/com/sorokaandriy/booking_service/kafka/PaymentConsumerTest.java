package com.sorokaandriy.booking_service.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.booking_service.dto.event.PaymentFailedEvent;
import com.sorokaandriy.booking_service.dto.event.PaymentSuccessEvent;
import com.sorokaandriy.booking_service.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentConsumerTest {

    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private BookingService service;
    private ObjectMapper objectMapper;
    private PaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        service = mock(BookingService.class);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        consumer = new PaymentConsumer(service, objectMapper);
    }

    @Test
    void paymentSuccessEvent_deserializesPayloadAndMarksBookingPaid() throws Exception {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId("pay_123")
                .amount(new BigDecimal("300.00"))
                .paidAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();

        consumer.paymentSuccessEvent(objectMapper.writeValueAsString(event));

        verify(service).markBookingAsPaid(event);
    }

    @Test
    void paymentSuccessEvent_throwsWhenPayloadIsNotDeserializable() {
        assertThatThrownBy(() -> consumer.paymentSuccessEvent("not-json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to deserialize payment-succeeded event");

        verifyNoInteractions(service);
    }

    @Test
    void paymentFailedEvent_deserializesPayloadAndMarksBookingCanceled() throws Exception {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId(UUID.fromString("77777777-7777-7777-7777-777777777777"))
                .reason("card_declined")
                .failedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();

        consumer.paymentFailedEvent(objectMapper.writeValueAsString(event));

        verify(service).markBookingAsCanceled(event);
    }

    @Test
    void paymentFailedEvent_throwsWhenPayloadIsNotDeserializable() {
        assertThatThrownBy(() -> consumer.paymentFailedEvent("not-json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to deserialize payment-failed event");

        verifyNoInteractions(service);
    }
}
