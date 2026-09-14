package com.sorokaandriy.booking_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.booking_service.dto.event.PaymentFailedEvent;
import com.sorokaandriy.booking_service.dto.event.PaymentSuccessEvent;
import com.sorokaandriy.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

    private final BookingService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.consumer.payment.succeed.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void paymentSuccessEvent(String payload){
        try {
            PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
            log.info("Received payment with id {}", event.paymentId());
            service.markBookingAsPaid(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize payment-succeeded event: {}", payload, ex);
            throw new RuntimeException("Failed to deserialize payment-succeeded event", ex);
        }
    }


    @KafkaListener(topics = "${kafka.consumer.payment.failed.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void paymentFailedEvent(String payload){
        try {
            PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
            log.info("Received failed payment with id {}", event.paymentId());
            service.markBookingAsCanceled(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize payment-failed event: {}", payload, ex);
            throw new RuntimeException("Failed to deserialize payment-failed event", ex);
        }
    }
}
