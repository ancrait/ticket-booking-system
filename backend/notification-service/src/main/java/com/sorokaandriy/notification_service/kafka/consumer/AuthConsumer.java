package com.sorokaandriy.notification_service.kafka.consumer;

import com.sorokaandriy.notification_service.dto.events.UserRegisteredEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthConsumer {

    private final EmailNotificationService service;


    @KafkaListener(topics = "${kafka.topic.user-registered}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserRegister(UserRegisteredEvent event) {
        log.info("User with id {} consumed", event.userId());
        service.handleUserRegister(event);
    }
}
