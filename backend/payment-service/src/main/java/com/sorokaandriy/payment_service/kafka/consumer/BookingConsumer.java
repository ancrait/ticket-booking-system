package com.sorokaandriy.payment_service.kafka.consumer;


import com.sorokaandriy.payment_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import javax.imageio.spi.ServiceRegistry;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingConsumer {

    private final PaymentService service;


    @KafkaListener(topics = "${kafka.consumer.booking.created.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingCreatedEvent(BookingCreatedEvent event){
        log.info("Received booking with id {}", event.bookingId());
        service.createPayment(event);
    }

    @KafkaListener(topics = "${kafka.consumer.booking.canceled.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingCanceled(BookingCanceledEvent event){
        log.info("Received booking with id {} for canceling", event.bookingId());
        service.cancelPayment(event.bookingId());

    }

    @KafkaListener(topics = "${kafka.consumer.booking.expired.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingExpired(BookingExpiredEvent event){
        log.info("Received booking with id {} for canceling expire booking", event.bookingId());
        service.cancelPayment(event.bookingId());
    }
}
