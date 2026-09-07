package com.sorokaandriy.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.payment_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.payment_service.entity.OutBox;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.exception.InvalidWebhookSignatureException;
import com.sorokaandriy.payment_service.exception.PaymentNotFoundException;
import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.sorokaandriy.payment_service.repository.OutBoxRepository;
import com.sorokaandriy.payment_service.repository.PaymentRepository;
import com.sorokaandriy.payment_service.service.mapper.PaymentMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    @Value("${kafka.producer.payment.success.topic}")
    private String paymentSuccess;
    @Value("${kafka.producer.payment.failed.topic}")
    private String paymentFailed;


    @Transactional
    public void handleWebhook(String payload, String sigHeader) {

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("payment_intent.succeeded".equals(event.getType())) {
                handlePaymentSucceeded(event);
            } else if ("payment_intent.payment_failed".equals(event.getType())) {
                handlePaymentFailed(event);
            } else {
                log.info("Unhandled Stripe event type: {}", event.getType());
            }

        }catch (SignatureVerificationException ex){
            log.error("Invalid Stripe webhook signature", ex);
            throw new InvalidWebhookSignatureException("Invalid signature", ex);
        }
    }


    @Transactional
    public void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (intent == null) {
            log.error("PaymentIntent is null in webhook");
            return;
        }

        Payment payment = repository.findByProviderPaymentId(intent.getId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for intent " + intent.getId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment {} already processed with status {}", payment.getId(), payment.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setUpdatedAt(Instant.now());
        repository.save(payment);

        PaymentSuccessEvent successEvent = mapper
                .fromPaymentToPaymentSuccessEvent(payment);

        OutBox outBox = OutBox.builder()
                .aggregateId(String.valueOf(payment.getId()))
                .topic(paymentSuccess)
                .payload(serialize(successEvent))
                .build();

        outBoxRepository.save(outBox);
    }



    @Transactional
    public void handlePaymentFailed(Event event){
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (intent == null) {
            log.error("PaymentIntent is null in webhook");
            return;
        }

        Payment payment = repository.findByProviderPaymentId(intent.getId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for intent " + intent.getId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment {} already processed with status {}, ignoring failed webhook",
                    payment.getId(), payment.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(Instant.now());
        repository.save(payment);

        PaymentFailedEvent failedEvent = mapper
                .fromPaymentToPaymentFailedEvent(payment);

        OutBox outBox = OutBox.builder()
                .aggregateId(String.valueOf(payment.getId()))
                .topic(paymentFailed)
                .payload(serialize(failedEvent))
                .build();

        outBoxRepository.save(outBox);
    }



    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new PaymentProcessingException("Failed to serialize event", ex);
        }
    }
}
