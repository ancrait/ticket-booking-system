package com.sorokaandriy.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sorokaandriy.notification_service.dto.events.UserRegisteredEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthConsumer {

    private final EmailNotificationService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.user-registered}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserRegister(String payload) {
        try {
            UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
            log.info("User with id {} consumed", event.userId());
            service.handleUserRegister(event);
        } catch (Exception ex) {
            log.error("Failed to deserialize UserRegisteredEvent: {}", payload, ex);
        }
    }
}
