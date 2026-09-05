package com.sorokaandriy.booking_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Value("${kafka.booking.created.topic}")
    private String createBookingTopic;
    @Value("${kafka.booking.expired.topic}")
    private String createBookingExpired;
    @Value("${kafka.booking.canceled.topic}")
    private String createBookingCanceled;

    @Bean
    public NewTopic bookingCreatedTopic() {
        return TopicBuilder.name(createBookingTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }


    @Bean
    public NewTopic bookingExpiredTopic() {
        return TopicBuilder.name(createBookingExpired)
                .partitions(1)
                .replicas(1)
                .build();
    }


    @Bean
    public NewTopic bookingCanceledTopic() {
        return TopicBuilder.name(createBookingCanceled)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
