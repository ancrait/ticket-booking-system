package com.sorokaandriy.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.payment_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.payment_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.payment_service.entity.OutBox;
import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.exception.InvalidWebhookSignatureException;
import com.sorokaandriy.payment_service.exception.PaymentNotFoundException;
import com.sorokaandriy.payment_service.repository.OutBoxRepository;
import com.sorokaandriy.payment_service.repository.PaymentRepository;
import com.sorokaandriy.payment_service.service.mapper.PaymentMapper;
import com.stripe.Stripe;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PROVIDER_PAYMENT_ID = "pi_123";
    private static final String WEBHOOK_SECRET = "whsec_test_secret";

    @Mock
    private PaymentRepository repository;

    @Mock
    private OutBoxRepository outBoxRepository;

    private ObjectMapper objectMapper;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        webhookService = new WebhookService(
                repository, new PaymentMapper(), outBoxRepository, objectMapper);
        ReflectionTestUtils.setField(webhookService, "webhookSecret", WEBHOOK_SECRET);
        ReflectionTestUtils.setField(webhookService, "paymentSuccess", "payment-success-topic");
        ReflectionTestUtils.setField(webhookService, "paymentFailed", "payment-failed-topic");
    }

    private Payment pendingPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .amount(new BigDecimal("99.99"))
                .status(PaymentStatus.PENDING)
                .providerPaymentId(PROVIDER_PAYMENT_ID)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    private String eventPayload(String type, String intentId) {
        return """
                {"id":"evt_123","object":"event","api_version":"%s","type":"%s","data":{"object":{"id":"%s","object":"payment_intent"}}}
                """.formatted(Stripe.API_VERSION, type, intentId).trim();
    }

    private String sign(String payload) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String v1 = HexFormat.of().formatHex(signature);
            return "t=" + timestamp + ",v1=" + v1;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void handleWebhook_marksPaymentSucceededAndWritesOutbox() throws Exception {
        String payload = eventPayload("payment_intent.succeeded", PROVIDER_PAYMENT_ID);
        when(repository.findByProviderPaymentId(PROVIDER_PAYMENT_ID))
                .thenReturn(Optional.of(pendingPayment()));

        webhookService.handleWebhook(payload, sign(payload));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(saved.getUpdatedAt()).isNotNull();

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("payment-success-topic");
        assertThat(outBox.getAggregateId()).isEqualTo(PAYMENT_ID.toString());

        PaymentSuccessEvent event =
                objectMapper.readValue(outBox.getPayload(), PaymentSuccessEvent.class);
        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID.toString());
        assertThat(event.amount()).isEqualByComparingTo("99.99");
        assertThat(event.paidAt()).isNotNull();
    }

    @Test
    void handleWebhook_marksPaymentFailedAndWritesOutbox() throws Exception {
        String payload = eventPayload("payment_intent.payment_failed", PROVIDER_PAYMENT_ID);
        when(repository.findByProviderPaymentId(PROVIDER_PAYMENT_ID))
                .thenReturn(Optional.of(pendingPayment()));

        webhookService.handleWebhook(payload, sign(payload));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("payment-failed-topic");

        PaymentFailedEvent event =
                objectMapper.readValue(outBox.getPayload(), PaymentFailedEvent.class);
        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.reason()).isEqualTo("Payment failed");
        assertThat(event.failedAt()).isNotNull();
    }

    @Test
    void handleWebhook_throwsInvalidSignatureOnBadSignature() {
        String payload = eventPayload("payment_intent.succeeded", PROVIDER_PAYMENT_ID);

        assertThatThrownBy(() -> webhookService.handleWebhook(payload, "t=1,v1=deadbeef"))
                .isInstanceOf(InvalidWebhookSignatureException.class)
                .hasMessage("Invalid signature");

        verifyNoInteractions(repository, outBoxRepository);
    }

    @Test
    void handleWebhook_ignoresUnhandledEventTypes() {
        String payload = eventPayload("charge.refunded", "ch_1");

        webhookService.handleWebhook(payload, sign(payload));

        verifyNoInteractions(repository, outBoxRepository);
    }

    @Test
    void handleWebhook_skipsAlreadyProcessedPayment() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.SUCCEEDED);
        String payload = eventPayload("payment_intent.succeeded", PROVIDER_PAYMENT_ID);
        when(repository.findByProviderPaymentId(PROVIDER_PAYMENT_ID))
                .thenReturn(Optional.of(payment));

        webhookService.handleWebhook(payload, sign(payload));

        verify(repository, never()).save(any());
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void handleWebhook_throwsWhenPaymentNotFound() {
        String payload = eventPayload("payment_intent.succeeded", PROVIDER_PAYMENT_ID);
        when(repository.findByProviderPaymentId(PROVIDER_PAYMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> webhookService.handleWebhook(payload, sign(payload)))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for intent");

        verifyNoInteractions(outBoxRepository);
    }
}
