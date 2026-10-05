package com.sorokaandriy.notification_service.service.mapper;

import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.notification_service.dto.events.UserRegisteredEvent;
import com.sorokaandriy.notification_service.entity.EmailNotification;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationStatus;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMapperTest {

    private static final String EMAIL = "john@example.com";
    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private EmailMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EmailMapper();
    }

    private void assertNotification(EmailNotification notification, NotificationType type) {
        assertThat(notification.getRecipientEmail()).isEqualTo(EMAIL);
        assertThat(notification.getSubject()).isEqualTo("subject");
        assertThat(notification.getBody()).isEqualTo("body");
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getErrorMessage()).isNull();
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void fromUserRegisteredEventToEmailNotificationMapsAllFields() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId("11111111-1111-1111-1111-111111111111")
                .email(EMAIL)
                .firstName("John")
                .lastName("Doe")
                .verificationToken("token-123")
                .registeredAt(Instant.now())
                .build();

        EmailNotification notification =
                mapper.fromUserRegisteredEventToEmailNotification(event, "subject", "body");

        assertNotification(notification, NotificationType.USER_REGISTERED);
    }

    @Test
    void fromPaymentSuccessEventToEmailNotificationMapsAllFields() {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email(EMAIL)
                .paymentId("pay_123")
                .amount(new BigDecimal("150.00"))
                .paidAt(Instant.now())
                .build();

        EmailNotification notification =
                mapper.fromPaymentSuccessEventToEmailNotification(event, "subject", "body");

        assertNotification(notification, NotificationType.TICKET_PURCHASED);
    }

    @Test
    void fromPaymentFailedEventToEmailNotificationMapsAllFields() {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email(EMAIL)
                .paymentId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .reason("card declined")
                .failedAt(Instant.now())
                .build();

        EmailNotification notification =
                mapper.fromPaymentFailedEventToEmailNotification(event, "subject", "body");

        assertNotification(notification, NotificationType.PAYMENT_FAILED);
    }

    @Test
    void fromBookingCanceledEventToEmailNotificationMapsAllFields() {
        BookingCanceledEvent event = BookingCanceledEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email(EMAIL)
                .eventId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .reason("canceled by user")
                .build();

        EmailNotification notification =
                mapper.fromBookingCanceledEventToEmailNotification(event, "subject", "body");

        assertNotification(notification, NotificationType.BOOKING_CANCELED);
    }

    @Test
    void fromBookingExpiredEventToEmailNotificationMapsAllFields() {
        BookingExpiredEvent event = BookingExpiredEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email(EMAIL)
                .eventId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .reason("payment not completed in time")
                .build();

        EmailNotification notification =
                mapper.fromBookingExpiredEventToEmailNotification(event, "subject", "body");

        assertNotification(notification, NotificationType.BOOKING_EXPIRED);
    }
}
