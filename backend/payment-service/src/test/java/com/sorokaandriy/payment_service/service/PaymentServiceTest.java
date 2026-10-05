package com.sorokaandriy.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.payment_service.client.BookingClient;
import com.sorokaandriy.payment_service.client.dto.BookingResponse;
import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.payment_service.dto.response.PaymentInitiateResponse;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.entity.OutBox;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.exception.PaymentNotFoundException;
import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.sorokaandriy.payment_service.repository.OutBoxRepository;
import com.sorokaandriy.payment_service.repository.PaymentRepository;
import com.sorokaandriy.payment_service.service.mapper.PaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PAYMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String PROVIDER_PAYMENT_ID = "pi_123";
    private static final String EMAIL = "john@example.com";
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");

    @Mock
    private PaymentRepository repository;

    @Mock
    private StripeService stripeService;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private OutBoxRepository outBoxRepository;

    private ObjectMapper objectMapper;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        paymentService = new PaymentService(
                repository,
                new PaymentMapper(),
                stripeService,
                bookingClient,
                outBoxRepository,
                objectMapper);
        ReflectionTestUtils.setField(paymentService, "currency", "UAH");
        ReflectionTestUtils.setField(paymentService, "paymentSuccess", "payment-success-topic");
    }

    private BookingCreatedEvent bookingCreatedEvent() {
        return BookingCreatedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .eventId(EVENT_ID)
                .eventTitle("Concert")
                .totalPrice(AMOUNT)
                .seatIds(List.of(UUID.randomUUID()))
                .build();
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .amount(AMOUNT)
                .status(status)
                .providerPaymentId(PROVIDER_PAYMENT_ID)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    private BookingResponse bookingResponse(String status, UUID ownerUserId) {
        return new BookingResponse(
                BOOKING_ID,
                ownerUserId,
                EMAIL,
                EVENT_ID,
                "Concert",
                status,
                new BigDecimal("150.00"),
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:10:00Z"));
    }

    private void stubSaveAssignsId() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(PAYMENT_ID);
            }
            return saved;
        });
    }

    @Test
    void createPayment_savesPendingPaymentWithProviderId() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());
        stubSaveAssignsId();
        when(stripeService.createPaymentIntent(AMOUNT, BOOKING_ID, USER_ID))
                .thenReturn("pi_created");

        paymentService.createPayment(bookingCreatedEvent());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(saved.getProviderPaymentId()).isEqualTo("pi_created");
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void createPayment_skipsWhenPaymentAlreadyExists() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        paymentService.createPayment(bookingCreatedEvent());

        verify(repository, never()).save(any());
        verifyNoInteractions(stripeService, outBoxRepository);
    }

    @Test
    void createPayment_marksPaymentFailedAndRethrowsWhenIntentCreationFails() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());
        stubSaveAssignsId();
        when(stripeService.createPaymentIntent(any(), eq(BOOKING_ID), eq(USER_ID)))
                .thenThrow(new PaymentProcessingException("Failed to create Stripe payment intent"));

        assertThatThrownBy(() -> paymentService.createPayment(bookingCreatedEvent()))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Failed to create Stripe payment intent");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository, times(2)).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(saved.getUpdatedAt()).isNotNull();
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void cancelPayment_cancelsPendingPaymentWithoutStripeCall() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));

        paymentService.cancelPayment(BOOKING_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getUpdatedAt()).isNotNull();
        verifyNoInteractions(stripeService);
    }

    @Test
    void cancelPayment_refundsSucceededPayment() {
        Payment payment = payment(PaymentStatus.SUCCEEDED);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));

        paymentService.cancelPayment(BOOKING_ID);

        verify(stripeService).refundPayment(PROVIDER_PAYMENT_ID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    void cancelPayment_skipsWhenPaymentAlreadyFinalized() {
        Payment payment = payment(PaymentStatus.CANCELED);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));

        paymentService.cancelPayment(BOOKING_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verifyNoInteractions(stripeService);
    }

    @Test
    void cancelPayment_throwsWhenPaymentNotFound() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(BOOKING_ID))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(BOOKING_ID.toString());

        verifyNoInteractions(stripeService);
    }

    @Test
    void initiatePayment_returnsClientSecretForPendingPayment() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));
        when(stripeService.resolvePaymentIntent(PROVIDER_PAYMENT_ID, AMOUNT, BOOKING_ID, USER_ID))
                .thenReturn(new StripeService.PaymentIntentResult(PROVIDER_PAYMENT_ID, false));
        when(stripeService.getClientSecret(PROVIDER_PAYMENT_ID)).thenReturn("cs_secret_123");
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateResponse response =
                paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER");

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.clientSecret()).isEqualTo("cs_secret_123");
        assertThat(response.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.currency()).isEqualTo("UAH");
        assertThat(response.alreadyPaid()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(repository).save(payment);
        verifyNoInteractions(outBoxRepository, bookingClient);
    }

    @Test
    void initiatePayment_updatesProviderIdWhenNewIntentCreated() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));
        when(stripeService.resolvePaymentIntent(PROVIDER_PAYMENT_ID, AMOUNT, BOOKING_ID, USER_ID))
                .thenReturn(new StripeService.PaymentIntentResult("pi_new", false));
        when(stripeService.getClientSecret("pi_new")).thenReturn("cs_secret_new");
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateResponse response =
                paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER");

        assertThat(response.clientSecret()).isEqualTo("cs_secret_new");
        assertThat(payment.getProviderPaymentId()).isEqualTo("pi_new");
        verify(stripeService).getClientSecret("pi_new");
    }

    @Test
    void initiatePayment_marksSucceededAndWritesOutboxWhenIntentAlreadySucceeded() throws Exception {
        Payment payment = payment(PaymentStatus.PENDING);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));
        when(stripeService.resolvePaymentIntent(PROVIDER_PAYMENT_ID, AMOUNT, BOOKING_ID, USER_ID))
                .thenReturn(new StripeService.PaymentIntentResult(PROVIDER_PAYMENT_ID, true));
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateResponse response =
                paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER");

        assertThat(response.alreadyPaid()).isTrue();
        assertThat(response.clientSecret()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(stripeService, never()).getClientSecret(anyString());

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("payment-success-topic");
        assertThat(outBox.getAggregateId()).isEqualTo(PAYMENT_ID.toString());
        PaymentSuccessEvent event =
                objectMapper.readValue(outBox.getPayload(), PaymentSuccessEvent.class);
        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID.toString());
        assertThat(event.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(event.paidAt()).isNotNull();
    }

    @Test
    void initiatePayment_returnsAlreadyPaidWhenPaymentSucceeded() {
        Payment payment = payment(PaymentStatus.SUCCEEDED);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));

        PaymentInitiateResponse response =
                paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER");

        assertThat(response.alreadyPaid()).isTrue();
        assertThat(response.clientSecret()).isNull();
        assertThat(response.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.currency()).isEqualTo("UAH");
        verify(repository, never()).save(any());
        verifyNoInteractions(stripeService, outBoxRepository, bookingClient);
    }

    @Test
    void initiatePayment_throwsWhenPaymentBelongsToAnotherUser() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> paymentService.initiatePayment(BOOKING_ID, OTHER_USER_ID, "USER"))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Payment does not belong to current user");

        verifyNoInteractions(stripeService, outBoxRepository, bookingClient);
    }

    @Test
    void initiatePayment_throwsWhenPaymentNotPending() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.CANCELED)));

        assertThatThrownBy(() -> paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER"))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Payment is not available for initiation. Current status: CANCELED");

        verifyNoInteractions(stripeService, outBoxRepository, bookingClient);
    }

    @Test
    void initiatePayment_createsPaymentFromBookingWhenMissing() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());
        when(bookingClient.getBooking(BOOKING_ID, USER_ID, "USER"))
                .thenReturn(bookingResponse("PENDING", USER_ID));
        BigDecimal bookingAmount = new BigDecimal("150.00");
        when(stripeService.createPaymentIntent(bookingAmount, BOOKING_ID, USER_ID))
                .thenReturn("pi_new");
        when(stripeService.resolvePaymentIntent("pi_new", bookingAmount, BOOKING_ID, USER_ID))
                .thenReturn(new StripeService.PaymentIntentResult("pi_new", false));
        when(stripeService.getClientSecret("pi_new")).thenReturn("cs_secret_new");
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(PAYMENT_ID);
            }
            return saved;
        });

        PaymentInitiateResponse response =
                paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER");

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.clientSecret()).isEqualTo("cs_secret_new");
        assertThat(response.amount()).isEqualByComparingTo(bookingAmount);
        assertThat(response.currency()).isEqualTo("UAH");
        assertThat(response.alreadyPaid()).isFalse();
        verify(bookingClient).getBooking(BOOKING_ID, USER_ID, "USER");
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository, times(3)).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getProviderPaymentId()).isEqualTo("pi_new");
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void initiatePayment_throwsWhenBookingNotFound() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());
        when(bookingClient.getBooking(BOOKING_ID, USER_ID, "USER")).thenReturn(null);

        assertThatThrownBy(() -> paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Booking not found for payment creation");

        verify(repository, never()).save(any());
        verifyNoInteractions(stripeService, outBoxRepository);
    }

    @Test
    void initiatePayment_throwsWhenBookingBelongsToAnotherUser() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());
        when(bookingClient.getBooking(BOOKING_ID, USER_ID, "USER"))
                .thenReturn(bookingResponse("PENDING", OTHER_USER_ID));

        assertThatThrownBy(() -> paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER"))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Booking does not belong to current user");

        verify(repository, never()).save(any());
        verifyNoInteractions(stripeService, outBoxRepository);
    }

    @Test
    void initiatePayment_throwsWhenBookingNotPending() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());
        when(bookingClient.getBooking(BOOKING_ID, USER_ID, "USER"))
                .thenReturn(bookingResponse("CANCELED", USER_ID));

        assertThatThrownBy(() -> paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER"))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Booking is not available for payment. Current status: CANCELED");

        verify(repository, never()).save(any());
        verifyNoInteractions(stripeService, outBoxRepository);
    }

    @Test
    void getPaymentStatus_returnsStatusResponse() {
        Payment payment = payment(PaymentStatus.SUCCEEDED);
        payment.setUpdatedAt(Instant.parse("2026-01-01T11:00:00Z"));
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.getPaymentStatus(BOOKING_ID, USER_ID);

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(response.currency()).isEqualTo("UAH");
        assertThat(response.providerPaymentId()).isEqualTo(PROVIDER_PAYMENT_ID);
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
    }

    @Test
    void getPaymentStatus_throwsWhenPaymentNotFound() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus(BOOKING_ID, USER_ID))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for booking");
    }

    @Test
    void getPaymentStatus_throwsWhenPaymentBelongsToAnotherUser() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> paymentService.getPaymentStatus(BOOKING_ID, OTHER_USER_ID))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Payment does not belong to current user");
    }

    @Test
    void refundPayment_refundsSucceededPayment() {
        Payment payment = payment(PaymentStatus.SUCCEEDED);
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.refundPayment(BOOKING_ID, USER_ID);

        verify(stripeService).refundPayment(PROVIDER_PAYMENT_ID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getUpdatedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.currency()).isEqualTo("UAH");
    }

    @Test
    void refundPayment_throwsWhenPaymentBelongsToAnotherUser() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.SUCCEEDED)));

        assertThatThrownBy(() -> paymentService.refundPayment(BOOKING_ID, OTHER_USER_ID))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Payment does not belong to current user");

        verifyNoInteractions(stripeService);
    }

    @Test
    void refundPayment_throwsWhenPaymentNotSucceeded() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThatThrownBy(() -> paymentService.refundPayment(BOOKING_ID, USER_ID))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Refund is only allowed for succeeded payments. Current status: PENDING");

        verify(stripeService, never()).refundPayment(anyString());
    }

    @Test
    void refundPayment_throwsWhenPaymentNotFound() {
        when(repository.findFirstByBookingIdOrderByCreatedAtDesc(BOOKING_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment(BOOKING_ID, USER_ID))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for booking");
    }
}
