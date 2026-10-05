package com.sorokaandriy.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.payment_service.dto.response.PaymentInitiateResponse;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import com.sorokaandriy.payment_service.exception.GlobalExceptionHandler;
import com.sorokaandriy.payment_service.exception.PaymentNotFoundException;
import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.sorokaandriy.payment_service.service.PaymentService;
import com.sorokaandriy.payment_service.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private PaymentService paymentService;
    private WebhookService webhookService;
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        webhookService = mock(WebhookService.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PaymentController(paymentService, webhookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private PaymentInitiateResponse initiateResponse() {
        return PaymentInitiateResponse.builder()
                .paymentId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .clientSecret("cs_secret_123")
                .amount(new BigDecimal("99.99"))
                .currency("UAH")
                .alreadyPaid(false)
                .build();
    }

    private PaymentStatusResponse statusResponse() {
        return PaymentStatusResponse.builder()
                .paymentId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .bookingId(BOOKING_ID)
                .status(PaymentStatus.SUCCEEDED)
                .amount(new BigDecimal("99.99"))
                .currency("UAH")
                .providerPaymentId("pi_123")
                .updatedAt(Instant.parse("2026-01-01T11:00:00Z"))
                .build();
    }

    @Test
    void initiatePayment_returnsInitiateResponse() throws Exception {
        when(paymentService.initiatePayment(BOOKING_ID, USER_ID, "USER"))
                .thenReturn(initiateResponse());

        mockMvc.perform(post("/api/payments/{bookingId}/initiate", BOOKING_ID)
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("33333333-3333-3333-3333-333333333333"))
                .andExpect(jsonPath("$.clientSecret").value("cs_secret_123"))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.currency").value("UAH"))
                .andExpect(jsonPath("$.alreadyPaid").value(false));

        verify(paymentService).initiatePayment(BOOKING_ID, USER_ID, "USER");
        verifyNoInteractions(webhookService);
    }

    @Test
    void initiatePayment_returns400WhenPaymentProcessingFails() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new PaymentProcessingException("Payment is not available for initiation"));

        mockMvc.perform(post("/api/payments/{bookingId}/initiate", BOOKING_ID)
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Payment is not available for initiation"));
    }

    @Test
    void getPaymentStatus_returnsStatusResponse() throws Exception {
        when(paymentService.getPaymentStatus(BOOKING_ID, USER_ID)).thenReturn(statusResponse());

        mockMvc.perform(get("/api/payments/{bookingId}/status", BOOKING_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId")
                        .value("33333333-3333-3333-3333-333333333333"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.currency").value("UAH"));

        verify(paymentService).getPaymentStatus(BOOKING_ID, USER_ID);
    }

    @Test
    void getPaymentStatus_returns404WhenPaymentNotFound() throws Exception {
        when(paymentService.getPaymentStatus(BOOKING_ID, USER_ID))
                .thenThrow(new PaymentNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/payments/{bookingId}/status", BOOKING_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Payment not found"));
    }

    @Test
    void refundPayment_returnsRefundedStatus() throws Exception {
        PaymentStatusResponse refunded = PaymentStatusResponse.builder()
                .status(PaymentStatus.REFUNDED)
                .currency("UAH")
                .build();
        when(paymentService.refundPayment(BOOKING_ID, USER_ID)).thenReturn(refunded);

        mockMvc.perform(post("/api/payments/{bookingId}/refund", BOOKING_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(paymentService).refundPayment(BOOKING_ID, USER_ID);
    }

    @Test
    void handleWebhook_returns204AndForwardsPayloadAndSignature() throws Exception {
        String payload = "{\"id\":\"evt_123\"}";
        String signature = "t=123,v1=abc";

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> signatureCaptor = ArgumentCaptor.forClass(String.class);
        verify(webhookService).handleWebhook(payloadCaptor.capture(), signatureCaptor.capture());
        assertThat(payloadCaptor.getValue()).isEqualTo(payload);
        assertThat(signatureCaptor.getValue()).isEqualTo(signature);
        verifyNoInteractions(paymentService);
    }
}
