package com.sorokaandriy.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentSuccessEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

    private final EmailNotificationService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.payment-success}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentSuccessEvent(String payload){
        try {
            PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
            log.info("Handle payment success event {}", event.paymentId());
            service.handlePaymentSuccess(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize PaymentSuccessEvent: {}", payload, ex);
        }
    }

    @KafkaListener(topics = "${kafka.topic.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailedEvent(String payload){
        try {
            PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
            log.info("Handle payment failed event {}", event.paymentId());
            service.handlePaymentFailed(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize PaymentFailedEvent: {}", payload, ex);
        }
    }
}
