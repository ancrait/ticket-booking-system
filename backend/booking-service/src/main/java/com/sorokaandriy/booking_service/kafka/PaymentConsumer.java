package com.sorokaandriy.booking_service.kafka;

import com.sorokaandriy.booking_service.dto.event.PaymentFailedEvent;
import com.sorokaandriy.booking_service.dto.event.PaymentSuccessEvent;
import com.sorokaandriy.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

    private final BookingService service;

    @KafkaListener(topics = "${kafka.consumer.payment.succeed.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void paymentSuccessEvent(PaymentSuccessEvent event){
        log.info("Received payment with id {}", event.paymentId());
        service.markBookingAsPaid(event);

    }


    @KafkaListener(topics = "${kafka.consumer.payment.failed.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void paymentFailedEvent(PaymentFailedEvent event){
        log.info("Received failed payment with id {}", event.paymentId());
        service.markBookingAsCanceled(event);

    }
}
