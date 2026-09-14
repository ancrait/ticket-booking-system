package com.sorokaandriy.payment_service.kafka.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.payment_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.payment_service.dto.events.BookingCreatedEvent;
import com.sorokaandriy.payment_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingConsumer {

    private final PaymentService service;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "${kafka.consumer.booking.created.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingCreatedEvent(String payload){
        try {
            BookingCreatedEvent event = objectMapper.readValue(payload, BookingCreatedEvent.class);
            log.info("Received booking with id {}", event.bookingId());
            service.createPayment(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize booking-created event: {}", payload, ex);
            throw new RuntimeException("Failed to deserialize booking-created event", ex);
        }
    }

    @KafkaListener(topics = "${kafka.consumer.booking.canceled.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingCanceled(String payload){
        try {
            BookingCanceledEvent event = objectMapper.readValue(payload, BookingCanceledEvent.class);
            log.info("Received booking with id {} for canceling", event.bookingId());
            service.cancelPayment(event.bookingId());
        } catch (Exception ex) {
            log.error("Failed to deserialize booking-canceled event: {}", payload, ex);
            throw new RuntimeException("Failed to deserialize booking-canceled event", ex);
        }
    }

    @KafkaListener(topics = "${kafka.consumer.booking.expired.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingExpired(String payload){
        try {
            BookingExpiredEvent event = objectMapper.readValue(payload, BookingExpiredEvent.class);
            log.info("Received booking with id {} for canceling expire booking", event.bookingId());
            service.cancelPayment(event.bookingId());
        } catch (Exception ex) {
            log.error("Failed to deserialize booking-expired event: {}", payload, ex);
            throw new RuntimeException("Failed to deserialize booking-expired event", ex);
        }
    }
}
