package com.sorokaandriy.payment_service.service;

import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class StripeService {

    @Value("${stripe.currency}")
    private String currency;

    public String createPaymentIntent(BigDecimal amount, UUID bookingId, UUID userId) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putMetadata("bookingId", bookingId.toString())
                    .putMetadata("userId", userId.toString())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            log.info("Created Stripe PaymentIntent {} for booking {}", intent.getId(), bookingId);

            return intent.getId();

        } catch (StripeException ex) {
            log.error("Failed to create Stripe PaymentIntent for booking {}", bookingId, ex);
            throw new PaymentProcessingException("Failed to create Stripe payment intent", ex);
        }
    }

    public String getClientSecret(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return intent.getClientSecret();
        } catch (StripeException ex) {
            log.error("Failed to retrieve Stripe PaymentIntent {}", paymentIntentId, ex);
            throw new PaymentProcessingException("Failed to retrieve payment intent", ex);
        }
    }

    public void refundPayment(String paymentIntentId) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build();

            Refund refund = Refund.create(params);

            log.info("Created Stripe refund {} for PaymentIntent {}", refund.getId(), paymentIntentId);

        } catch (StripeException ex) {
            log.error("Failed to create Stripe refund for PaymentIntent {}", paymentIntentId, ex);
            throw new PaymentProcessingException("Failed to refund payment", ex);
        }
    }
}
