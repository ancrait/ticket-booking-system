package com.sorokaandriy.payment_service.service;

import com.sorokaandriy.payment_service.dto.response.PaymentInitiateResponse;
import com.sorokaandriy.payment_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.exception.PaymentNotFoundException;
import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.sorokaandriy.payment_service.repository.PaymentRepository;
import com.sorokaandriy.payment_service.service.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final StripeService stripeService;

    @Value("${stripe.currency}")
    private String currency;


    @Transactional
    public void createPayment(BookingCreatedEvent event) {

        if (repository.existsPaymentByBookingId(event.bookingId())) {
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

        Payment payment = repository.getPaymentByBookingId(bookingId)
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

    @Transactional(readOnly = true)
    public PaymentInitiateResponse initiatePayment(UUID bookingId, UUID currentUserId) {
        Payment payment = repository.getPaymentByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking " + bookingId));

        if (!payment.getUserId().equals(currentUserId)) {
            throw new PaymentProcessingException("Payment does not belong to current user");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentProcessingException(
                    "Payment is not available for initiation. Current status: " + payment.getStatus());
        }

        String clientSecret = stripeService.getClientSecret(payment.getProviderPaymentId());

        return PaymentInitiateResponse.builder()
                .paymentId(payment.getId())
                .clientSecret(clientSecret)
                .amount(payment.getAmount())
                .currency(currency)
                .build();
    }


    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID bookingId, UUID userId) {

        Payment payment = repository.getPaymentByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking " + bookingId));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentProcessingException("Payment does not belong to current user");
        }

        return mapper.fromPaymentToPaymentStatusResponse(payment, currency);
    }


    @Transactional
    public PaymentStatusResponse refundPayment(UUID bookingId, UUID userId) {

        Payment payment = repository.getPaymentByBookingId(bookingId)
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
