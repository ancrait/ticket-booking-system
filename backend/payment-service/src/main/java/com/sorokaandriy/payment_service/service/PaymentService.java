package com.sorokaandriy.payment_service.service;

import com.sorokaandriy.payment_service.client.BookingClient;
import com.sorokaandriy.payment_service.client.dto.BookingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.payment_service.dto.response.PaymentInitiateResponse;
import com.sorokaandriy.payment_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.entity.OutBox;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.exception.PaymentNotFoundException;
import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.sorokaandriy.payment_service.repository.OutBoxRepository;
import com.sorokaandriy.payment_service.repository.PaymentRepository;
import com.sorokaandriy.payment_service.service.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final StripeService stripeService;
    private final BookingClient bookingClient;
    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @Value("${stripe.currency}")
    private String currency;

    @Value("${kafka.producer.payment.success.topic}")
    private String paymentSuccess;


    @Transactional
    public void createPayment(BookingCreatedEvent event) {

        if (repository.findFirstByBookingIdOrderByCreatedAtDesc(event.bookingId()).isPresent()) {
            log.info("Payment for booking {} already exists", event.bookingId());
            return;
        }

        Payment payment = repository.save(mapper.fromBookingCreatedEventToPayment(event));

        try {
            String providerPaymentId = stripeService.createPaymentIntent(
                    payment.getAmount(),
                    payment.getBookingId(),
                    payment.getUserId()
            );

            payment.setProviderPaymentId(providerPaymentId);
            payment.setUpdatedAt(Instant.now());

            log.info("Payment pending for booking {} with provider id {}", payment.getBookingId(), providerPaymentId);

        } catch (PaymentProcessingException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(Instant.now());
            repository.save(payment);
            log.error("Failed to create payment for booking {}", payment.getBookingId(), ex);
            throw ex;
        }
    }


    @Transactional
    public void cancelPayment(UUID bookingId) {

        Payment payment = repository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with bookingId " +
                        bookingId + " does not exist"));

        if (payment.getStatus() == PaymentStatus.CANCELED ||
            payment.getStatus() == PaymentStatus.REFUNDED ||
            payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment for booking {} already finalized with status {}",
                    bookingId, payment.getStatus());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELED);
            log.info("Payment for booking {} canceled", bookingId);

        } else if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            stripeService.refundPayment(payment.getProviderPaymentId());
            payment.setStatus(PaymentStatus.REFUNDED);
            log.info("Payment for booking {} refunded", bookingId);
        }

        payment.setUpdatedAt(Instant.now());
    }

    @Transactional
    public PaymentInitiateResponse initiatePayment(UUID bookingId, UUID currentUserId, String role) {
        Payment payment = repository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseGet(() -> createPaymentFromBooking(bookingId, currentUserId, role));

        if (!payment.getUserId().equals(currentUserId)) {
            throw new PaymentProcessingException("Payment does not belong to current user");
        }

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return PaymentInitiateResponse.builder()
                    .paymentId(payment.getId())
                    .clientSecret(null)
                    .amount(payment.getAmount())
                    .currency(currency)
                    .alreadyPaid(true)
                    .build();
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentProcessingException(
                    "Payment is not available for initiation. Current status: " + payment.getStatus());
        }

        StripeService.PaymentIntentResult result = stripeService.resolvePaymentIntent(
                payment.getProviderPaymentId(), payment.getAmount(), payment.getBookingId(), payment.getUserId());

        if (!result.id().equals(payment.getProviderPaymentId())) {
            payment.setProviderPaymentId(result.id());
        }
        payment.setUpdatedAt(Instant.now());

        if (result.alreadySucceeded()) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            PaymentSuccessEvent successEvent = mapper.fromPaymentToPaymentSuccessEvent(payment);
            OutBox outBox = OutBox.builder()
                    .aggregateId(String.valueOf(payment.getId()))
                    .topic(paymentSuccess)
                    .payload(serialize(successEvent))
                    .build();
            outBoxRepository.save(outBox);
            repository.save(payment);
            log.info("PaymentIntent for booking {} already succeeded; marked payment {} as succeeded",
                    bookingId, payment.getId());
            return PaymentInitiateResponse.builder()
                    .paymentId(payment.getId())
                    .clientSecret(null)
                    .amount(payment.getAmount())
                    .currency(currency)
                    .alreadyPaid(true)
                    .build();
        }

        repository.save(payment);

        String clientSecret = stripeService.getClientSecret(result.id());

        return PaymentInitiateResponse.builder()
                .paymentId(payment.getId())
                .clientSecret(clientSecret)
                .amount(payment.getAmount())
                .currency(currency)
                .alreadyPaid(false)
                .build();
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new PaymentProcessingException("Failed to serialize event", ex);
        }
    }

    private Payment createPaymentFromBooking(UUID bookingId, UUID currentUserId, String role) {
        BookingResponse booking = bookingClient.getBooking(bookingId, currentUserId, role);
        if (booking == null) {
            throw new PaymentNotFoundException("Booking not found for payment creation: " + bookingId);
        }

        if (!booking.userId().equals(currentUserId)) {
            throw new PaymentProcessingException("Booking does not belong to current user");
        }

        if (!"PENDING".equalsIgnoreCase(booking.status())) {
            throw new PaymentProcessingException(
                    "Booking is not available for payment. Current status: " + booking.status());
        }

        Optional<Payment> existingPayment = repository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(currentUserId)
                .email(booking.email())
                .amount(booking.totalPrice())
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        payment = repository.save(payment);

        try {
            String providerPaymentId = stripeService.createPaymentIntent(
                    payment.getAmount(), payment.getBookingId(), payment.getUserId());
            payment.setProviderPaymentId(providerPaymentId);
            payment.setUpdatedAt(Instant.now());
        } catch (PaymentProcessingException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(Instant.now());
            repository.save(payment);
            log.error("Failed to create payment for booking {}", payment.getBookingId(), ex);
            throw ex;
        }

        return repository.save(payment);
    }


    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID bookingId, UUID userId) {

        Payment payment = repository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking " + bookingId));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentProcessingException("Payment does not belong to current user");
        }

        return mapper.fromPaymentToPaymentStatusResponse(payment, currency);
    }


    @Transactional
    public PaymentStatusResponse refundPayment(UUID bookingId, UUID userId) {

        Payment payment = repository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking " + bookingId));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentProcessingException("Payment does not belong to current user");
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new PaymentProcessingException(
                    "Refund is only allowed for succeeded payments. Current status: " + payment.getStatus());
        }

        stripeService.refundPayment(payment.getProviderPaymentId());
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(Instant.now());

        return mapper.fromPaymentToPaymentStatusResponse(payment, currency);
    }
}
