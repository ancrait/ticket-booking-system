package com.sorokaandriy.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfig {

    @Value("${kafka.producer.payment.success.topic}")
    private String successPaymentTopic;
    @Value("${kafka.producer.payment.failed.topic}")
    private String failedPaymentTopic;

    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name(successPaymentTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }


    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(failedPaymentTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
