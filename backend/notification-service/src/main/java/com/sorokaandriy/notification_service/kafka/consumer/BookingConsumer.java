package com.sorokaandriy.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingConsumer {

    private final EmailNotificationService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.booking-canceled}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingCanceledEvent(String payload){
        try {
            BookingCanceledEvent event = objectMapper.readValue(payload, BookingCanceledEvent.class);
            log.info("Handle BookingCanceledEvent {}", event.bookingId());
            service.handleBookingCanceledEvent(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize BookingCanceledEvent: {}", payload, ex);
        }
    }

    @KafkaListener(topics = "${kafka.topic.booking-expired}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingExpiredEvent(String payload){
        try {
            BookingExpiredEvent event = objectMapper.readValue(payload, BookingExpiredEvent.class);
            log.info("Handle BookingExpiredEvent {}", event.bookingId());
            service.handleBookingExpiredEvent(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize BookingExpiredEvent: {}", payload, ex);
        }
    }
}
