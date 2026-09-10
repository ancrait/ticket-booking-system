package com.sorokaandriy.notification_service.service.mapper;

import com.sorokaandriy.notification_service.dto.events.*;
import com.sorokaandriy.notification_service.entity.EmailNotification;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationStatus;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class EmailMapper {

    public EmailNotification fromUserRegisteredEventToEmailNotification(UserRegisteredEvent event,
                                                                        String subject, String body){
        return EmailNotification.builder()
                .recipientEmail(event.email())
                .subject(subject)
                .body(body)
                .type(NotificationType.USER_REGISTERED)
                .status(NotificationStatus.PENDING)
                .build();
    }

    public EmailNotification fromPaymentSuccessEventToEmailNotification(PaymentSuccessEvent event,
                                                                        String subject, String body){
        return EmailNotification.builder()
                .recipientEmail(event.email())
                .subject(subject)
                .body(body)
                .type(NotificationType.TICKET_PURCHASED)
                .status(NotificationStatus.PENDING)
                .build();
    }

    public EmailNotification fromPaymentFailedEventToEmailNotification(PaymentFailedEvent event,
                                                                       String subject, String body){
        return EmailNotification.builder()
                .recipientEmail(event.email())
                .subject(subject)
                .body(body)
                .type(NotificationType.PAYMENT_FAILED)
                .status(NotificationStatus.PENDING)
                .build();
    }

    public EmailNotification fromBookingCanceledEventToEmailNotification(BookingCanceledEvent event,
                                                                         String subject, String body){
        return EmailNotification.builder()
                .recipientEmail(event.email())
                .subject(subject)
                .body(body)
                .type(NotificationType.BOOKING_CANCELED)
                .status(NotificationStatus.PENDING)
                .build();
    }

    public EmailNotification fromBookingExpiredEventToEmailNotification(BookingExpiredEvent event,
                                                                        String subject, String body){
        return EmailNotification.builder()
                .recipientEmail(event.email())
                .subject(subject)
                .body(body)
                .type(NotificationType.BOOKING_EXPIRED)
                .status(NotificationStatus.PENDING)
                .build();
    }
}
