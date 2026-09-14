package com.sorokaandriy.payment_service.controller;

import com.sorokaandriy.payment_service.dto.response.PaymentInitiateResponse;
import com.sorokaandriy.payment_service.dto.response.PaymentStatusResponse;
import com.sorokaandriy.payment_service.service.PaymentService;
import com.sorokaandriy.payment_service.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final WebhookService webhookService;

    @PostMapping("/{bookingId}/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role
    ) {
        return ResponseEntity.ok(paymentService.initiatePayment(bookingId, userId, role));
    }

    @GetMapping("/{bookingId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId
    ){
        return ResponseEntity.ok(paymentService.getPaymentStatus(bookingId, userId));
    }

    @PostMapping("/{bookingId}/refund")
    public ResponseEntity<PaymentStatusResponse> refundPayment(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId
    ){
        return ResponseEntity.ok(paymentService.refundPayment(bookingId, userId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        webhookService.handleWebhook(payload, sigHeader);
        return ResponseEntity.noContent().build();
    }


}
