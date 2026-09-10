package com.sorokaandriy.notification_service.kafka.consumer;

import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.entity.EmailNotification;
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


    @KafkaListener(topics = "${kafka.topic.booking-canceled}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingCanceledEvent(BookingCanceledEvent event){
        log.info("Handle BookingCanceledEvent {}", event.bookingId());
        service.handleBookingCanceledEvent(event);
    }

    @KafkaListener(topics = "${kafka.topic.booking-expired}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingExpiredEvent(BookingExpiredEvent event){
        log.info("Handle BookingExpiredEvent {}", event.bookingId());
        service.handleBookingExpiredEvent(event);
    }
}
