package com.sorokaandriy.notification_service.service;

import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentSuccessEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlBodyServiceTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAYMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final HtmlBodyService htmlBodyService = new HtmlBodyService();

    @Test
    void buildRegistrationEmailBody_containsNameAndVerificationLink() {
        String body = htmlBodyService.buildRegistrationEmailBody(
                "John", "Doe", "http://localhost:5173/verify-email?token=abc-123");

        assertThat(body).contains("Welcome to TIQ, John Doe!");
        assertThat(body).contains("http://localhost:5173/verify-email?token=abc-123");
        assertThat(body).contains("Verify email");
    }

    @Test
    void buildPaymentSuccessEmailBody_containsEventDetails() {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .paymentId("pay-123")
                .amount(new BigDecimal("250.00"))
                .paidAt(Instant.parse("2026-01-01T11:00:00Z"))
                .build();

        String body = htmlBodyService.buildPaymentSuccessEmailBody(event);

        assertThat(body).contains(BOOKING_ID.toString());
        assertThat(body).contains("pay-123");
        assertThat(body).contains("250.00");
        assertThat(body).contains("2026-01-01T11:00:00Z");
    }

    @Test
    void buildPaymentFailedEmailBody_containsEventDetails() {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .paymentId(PAYMENT_ID)
                .reason("card_declined")
                .failedAt(Instant.parse("2026-01-01T11:00:00Z"))
                .build();

        String body = htmlBodyService.buildPaymentFailedEmailBody(event);

        assertThat(body).contains(BOOKING_ID.toString());
        assertThat(body).contains(PAYMENT_ID.toString());
        assertThat(body).contains("card_declined");
        assertThat(body).contains("2026-01-01T11:00:00Z");
    }

    @Test
    void buildBookingCanceledEmailBody_containsEventDetails() {
        BookingCanceledEvent event = BookingCanceledEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .eventId(EVENT_ID)
                .reason("user_request")
                .build();

        String body = htmlBodyService.buildBookingCanceledEmailBody(event);

        assertThat(body).contains(BOOKING_ID.toString());
        assertThat(body).contains(EVENT_ID.toString());
        assertThat(body).contains("user_request");
    }

    @Test
    void buildBookingExpiredEmailBody_containsEventDetails() {
        BookingExpiredEvent event = BookingExpiredEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .eventId(EVENT_ID)
                .reason("payment_timeout")
                .build();

        String body = htmlBodyService.buildBookingExpiredEmailBody(event);

        assertThat(body).contains(BOOKING_ID.toString());
        assertThat(body).contains(EVENT_ID.toString());
        assertThat(body).contains("payment_timeout");
    }
}
