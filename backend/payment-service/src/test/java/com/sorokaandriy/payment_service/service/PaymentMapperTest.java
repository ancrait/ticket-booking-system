package com.sorokaandriy.payment_service.service;

import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.service.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAYMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final PaymentMapper mapper = new PaymentMapper();

    private BookingCreatedEvent bookingCreatedEvent() {
        return BookingCreatedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .eventId(UUID.randomUUID())
                .eventTitle("Concert")
                .totalPrice(new BigDecimal("99.99"))
                .seatIds(List.of(UUID.randomUUID()))
                .build();
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .amount(new BigDecimal("99.99"))
                .status(status)
                .providerPaymentId("pi_123")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T11:00:00Z"))
                .build();
    }

    @Test
    void fromBookingCreatedEventToPayment_mapsAllFields() {
        Payment payment = mapper.fromBookingCreatedEventToPayment(bookingCreatedEvent());

        assertThat(payment.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(payment.getUserId()).isEqualTo(USER_ID);
        assertThat(payment.getEmail()).isEqualTo("john@example.com");
        assertThat(payment.getAmount()).isEqualByComparingTo("99.99");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getId()).isNull();
        assertThat(payment.getProviderPaymentId()).isNull();
    }

    @Test
    void fromPaymentToPaymentSuccessEvent_mapsAllFields() {
        Payment payment = payment(PaymentStatus.SUCCEEDED);

        PaymentSuccessEvent event = mapper.fromPaymentToPaymentSuccessEvent(payment);

        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID.toString());
        assertThat(event.amount()).isEqualByComparingTo("99.99");
        assertThat(event.paidAt()).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
    }

    @Test
    void fromPaymentToPaymentFailedEvent_mapsAllFields() {
        Payment payment = payment(PaymentStatus.FAILED);

        PaymentFailedEvent event = mapper.fromPaymentToPaymentFailedEvent(payment);

        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.reason()).isEqualTo("Payment failed");
        assertThat(event.failedAt()).isNotNull();
    }

    @Test
    void fromPaymentToPaymentStatusResponse_mapsAllFieldsWithCurrency() {
        Payment payment = payment(PaymentStatus.SUCCEEDED);

        PaymentStatusResponse response =
                mapper.fromPaymentToPaymentStatusResponse(payment, "UAH");

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.amount()).isEqualByComparingTo("99.99");
        assertThat(response.currency()).isEqualTo("UAH");
        assertThat(response.providerPaymentId()).isEqualTo("pi_123");
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
    }
}
