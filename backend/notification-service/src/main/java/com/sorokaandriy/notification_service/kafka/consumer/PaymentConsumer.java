package com.sorokaandriy.notification_service.kafka.consumer;

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

    @KafkaListener(topics = "${kafka.topic.payment-success}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event){
        log.info("Handle payment success event {}", event.paymentId());
        service.handlePaymentSuccess(event);
    }

    @KafkaListener(topics = "${kafka.topic.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailedEvent(PaymentFailedEvent event){
        log.info("Handle payment failed event {}", event.paymentId());
        service.handlePaymentFailed(event);
    }
}
