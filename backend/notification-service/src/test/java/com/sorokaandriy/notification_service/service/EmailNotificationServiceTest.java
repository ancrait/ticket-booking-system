package com.sorokaandriy.notification_service.service;

import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.notification_service.dto.events.UserRegisteredEvent;
import com.sorokaandriy.notification_service.entity.EmailNotification;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationStatus;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationType;
import com.sorokaandriy.notification_service.repository.EmailNotificationRepository;
import com.sorokaandriy.notification_service.service.mapper.EmailMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    private static final String FRONTEND_URL = "http://localhost:5173";
    private static final String EMAIL = "john@example.com";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PAYMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private EmailNotificationRepository repository;

    @Mock
    private EmailService emailService;

    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailNotificationService(
                repository,
                new EmailMapper(),
                emailService,
                new HtmlBodyService());
        ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
    }

    private UserRegisteredEvent userRegisteredEvent() {
        return UserRegisteredEvent.builder()
                .userId(USER_ID.toString())
                .email(EMAIL)
                .firstName("John")
                .lastName("Doe")
                .verificationToken("token-123")
                .registeredAt(Instant.parse("2026-10-04T10:00:00Z"))
                .build();
    }

    private PaymentSuccessEvent paymentSuccessEvent() {
        return PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .paymentId("pay_123")
                .amount(new BigDecimal("150.00"))
                .paidAt(Instant.parse("2026-10-04T12:00:00Z"))
                .build();
    }

    private PaymentFailedEvent paymentFailedEvent() {
        return PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .paymentId(PAYMENT_ID)
                .reason("card declined")
                .failedAt(Instant.parse("2026-10-04T12:30:00Z"))
                .build();
    }

    private BookingCanceledEvent bookingCanceledEvent() {
        return BookingCanceledEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .eventId(EVENT_ID)
                .reason("canceled by user")
                .build();
    }

    private BookingExpiredEvent bookingExpiredEvent() {
        return BookingExpiredEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .eventId(EVENT_ID)
                .reason("payment not completed in time")
                .build();
    }

    private EmailNotification lastSavedNotification() {
        ArgumentCaptor<EmailNotification> captor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository, times(2)).save(captor.capture());
        return captor.getAllValues().get(1);
    }

    private void stubEmailFailure() throws MessagingException {
        doThrow(new MessagingException("smtp down")).when(emailService)
                .sendHtmlEmail(anyString(), anyString(), anyString());
    }

    @Test
    void handleUserRegisterSendsVerificationEmailAndSavesSentNotification() throws MessagingException {
        UserRegisteredEvent event = userRegisteredEvent();

        service.handleUserRegister(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq(EMAIL), eq("Welcome to TIQ!"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains(FRONTEND_URL + "/verify-email?token=token-123")
                .contains("John")
                .contains("Doe");

        EmailNotification lastSave = lastSavedNotification();

        assertThat(lastSave.getRecipientEmail()).isEqualTo(EMAIL);
        assertThat(lastSave.getSubject()).isEqualTo("Welcome to TIQ!");
        assertThat(lastSave.getType()).isEqualTo(NotificationType.USER_REGISTERED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(lastSave.getSentAt()).isNotNull();
        assertThat(lastSave.getErrorMessage()).isNull();
    }

    @Test
    void handleUserRegisterSavesFailedNotificationWhenEmailSendingFails() throws MessagingException {
        stubEmailFailure();

        assertThatCode(() -> service.handleUserRegister(userRegisteredEvent()))
                .doesNotThrowAnyException();

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(lastSave.getErrorMessage()).isEqualTo("smtp down");
        assertThat(lastSave.getSentAt()).isNull();
    }

    @Test
    void handlePaymentSuccessSendsEmailAndSavesSentNotification() throws MessagingException {
        PaymentSuccessEvent event = paymentSuccessEvent();

        service.handlePaymentSuccess(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq(EMAIL), eq("Payment Success"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains(BOOKING_ID.toString())
                .contains("pay_123")
                .contains("150.00");

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getRecipientEmail()).isEqualTo(EMAIL);
        assertThat(lastSave.getType()).isEqualTo(NotificationType.TICKET_PURCHASED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(lastSave.getSentAt()).isNotNull();
    }

    @Test
    void handlePaymentSuccessSavesFailedNotificationWhenEmailSendingFails() throws MessagingException {
        stubEmailFailure();

        assertThatCode(() -> service.handlePaymentSuccess(paymentSuccessEvent()))
                .doesNotThrowAnyException();

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.TICKET_PURCHASED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(lastSave.getErrorMessage()).isEqualTo("smtp down");
    }

    @Test
    void handlePaymentFailedSendsEmailAndSavesSentNotification() throws MessagingException {
        PaymentFailedEvent event = paymentFailedEvent();

        service.handlePaymentFailed(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq(EMAIL), eq("Payment Failed"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains(BOOKING_ID.toString())
                .contains("card declined");

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.PAYMENT_FAILED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(lastSave.getSentAt()).isNotNull();
    }

    @Test
    void handlePaymentFailedSavesFailedNotificationWhenEmailSendingFails() throws MessagingException {
        stubEmailFailure();

        assertThatCode(() -> service.handlePaymentFailed(paymentFailedEvent()))
                .doesNotThrowAnyException();

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.PAYMENT_FAILED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(lastSave.getErrorMessage()).isEqualTo("smtp down");
    }

    @Test
    void handleBookingCanceledEventSendsEmailAndSavesSentNotification() throws MessagingException {
        BookingCanceledEvent event = bookingCanceledEvent();

        service.handleBookingCanceledEvent(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq(EMAIL), eq("Booking Canceled"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains(BOOKING_ID.toString())
                .contains(EVENT_ID.toString())
                .contains("canceled by user");

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.BOOKING_CANCELED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(lastSave.getSentAt()).isNotNull();
    }

    @Test
    void handleBookingCanceledEventSavesFailedNotificationWhenEmailSendingFails() throws MessagingException {
        stubEmailFailure();

        assertThatCode(() -> service.handleBookingCanceledEvent(bookingCanceledEvent()))
                .doesNotThrowAnyException();

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.BOOKING_CANCELED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(lastSave.getErrorMessage()).isEqualTo("smtp down");
    }

    @Test
    void handleBookingExpiredEventSendsEmailAndSavesSentNotification() throws MessagingException {
        BookingExpiredEvent event = bookingExpiredEvent();

        service.handleBookingExpiredEvent(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq(EMAIL), eq("Booking Expired"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains(BOOKING_ID.toString())
                .contains(EVENT_ID.toString())
                .contains("payment not completed in time");

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.BOOKING_EXPIRED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(lastSave.getSentAt()).isNotNull();
    }

    @Test
    void handleBookingExpiredEventSavesFailedNotificationWhenEmailSendingFails() throws MessagingException {
        stubEmailFailure();

        assertThatCode(() -> service.handleBookingExpiredEvent(bookingExpiredEvent()))
                .doesNotThrowAnyException();

        EmailNotification lastSave = lastSavedNotification();
        assertThat(lastSave.getType()).isEqualTo(NotificationType.BOOKING_EXPIRED);
        assertThat(lastSave.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(lastSave.getErrorMessage()).isEqualTo("smtp down");
    }
}
