package com.sorokaandriy.payment_service.service.mapper;

import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentMapper {

    public Payment fromBookingCreatedEventToPayment(
            BookingCreatedEvent event){

        return Payment.builder()
                .bookingId(event.bookingId())
                .userId(event.userId())
                .amount(event.totalPrice())
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public PaymentSuccessEvent fromPaymentToPaymentSuccessEvent(Payment payment){
        return PaymentSuccessEvent.builder()
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .paymentId(String.valueOf(payment.getId()))
                .amount(payment.getAmount())
                .paidAt(payment.getUpdatedAt())
                .build();
    }

    public PaymentFailedEvent fromPaymentToPaymentFailedEvent(Payment payment) {

        return PaymentFailedEvent.builder()
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .paymentId(payment.getId())
                .reason("Payment failed")
                .failedAt(Instant.now())
                .build();
    }


    public PaymentStatusResponse fromPaymentToPaymentStatusResponse(Payment payment, String currency) {

        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(currency)
                .providerPaymentId(payment.getProviderPaymentId())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
