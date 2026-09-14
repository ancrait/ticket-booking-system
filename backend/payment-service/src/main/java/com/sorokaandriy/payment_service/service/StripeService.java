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
import java.util.List;
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
                    .addAllPaymentMethodType(List.of("card"))
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

    public PaymentIntentResult resolvePaymentIntent(String currentPaymentIntentId,
                                                     BigDecimal amount,
                                                     UUID bookingId,
                                                     UUID userId) {
        if (currentPaymentIntentId != null && !currentPaymentIntentId.isBlank()) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(currentPaymentIntentId);
                List<String> types = intent.getPaymentMethodTypes();
                String status = intent.getStatus();

                log.info("Inspecting PaymentIntent {} for booking {}: types={}, status={}",
                        currentPaymentIntentId, bookingId, types, status);

                boolean isCardOnly = types != null && types.size() == 1 && "card".equals(types.get(0));
                boolean isConfirmable = "requires_payment_method".equals(status)
                        || "requires_confirmation".equals(status);

                if (isCardOnly && isConfirmable) {
                    return new PaymentIntentResult(currentPaymentIntentId, false);
                }

                if ("succeeded".equals(status)) {
                    log.info("PaymentIntent {} for booking {} already succeeded", currentPaymentIntentId, bookingId);
                    return new PaymentIntentResult(currentPaymentIntentId, true);
                }

                if (isConfirmable) {
                    intent.cancel();
                    log.info("Canceled old PaymentIntent {} for booking {}", currentPaymentIntentId, bookingId);
                }
            } catch (StripeException ex) {
                log.warn("Failed to inspect/cancel old PaymentIntent {}, will create a new one",
                        currentPaymentIntentId, ex);
            }
        }

        return new PaymentIntentResult(createPaymentIntent(amount, bookingId, userId), false);
    }

    public record PaymentIntentResult(String id, boolean alreadySucceeded) {
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
