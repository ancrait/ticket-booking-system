package com.sorokaandriy.notification_service.service;

import com.sorokaandriy.notification_service.dto.events.*;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.entity.EmailNotification;
import com.sorokaandriy.notification_service.entity.enumiration.NotificationStatus;
import com.sorokaandriy.notification_service.repository.EmailNotificationRepository;
import com.sorokaandriy.notification_service.service.mapper.EmailMapper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService {

    private final EmailNotificationRepository repository;
    private final EmailMapper mapper;
    private final EmailService emailService;
    private final HtmlBodyService htmlBodyService;

    @Value("${app.frontend-url}")
    private String frontendUrl;


    @Transactional
    public void handleUserRegister(UserRegisteredEvent event) {
        String subject = "Welcome to TIQ!";
        String verificationLink = frontendUrl + "/verify-email?token=" + event.verificationToken();
        String body = htmlBodyService.buildRegistrationEmailBody(event.firstName(), event.lastName(), verificationLink);

        EmailNotification notification = mapper
                .fromUserRegisteredEventToEmailNotification(event, subject, body);

        repository.save(notification);

        try {
            emailService.sendHtmlEmail(event.email(), subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (MessagingException ex) {
            log.error("Failed to send registration email to {}", event.email(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        repository.save(notification);
    }


    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        String subject = "Payment Success";
        String body = htmlBodyService.buildPaymentSuccessEmailBody(event);

        EmailNotification notification = mapper
                .fromPaymentSuccessEventToEmailNotification(event, subject, body);

        repository.save(notification);

        try {
            emailService.sendHtmlEmail(event.email(), subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (MessagingException ex) {
            log.error("Failed to send payment success email to {}", event.email(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        repository.save(notification);

    }


    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        String subject = "Payment Failed";
        String body = htmlBodyService.buildPaymentFailedEmailBody(event);

        EmailNotification notification = mapper
                .fromPaymentFailedEventToEmailNotification(event, subject, body);

        repository.save(notification);

        try {
            emailService.sendHtmlEmail(event.email(), subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (MessagingException ex) {
            log.error("Failed to send payment failed email to {}", event.email(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        repository.save(notification);

    }


    @Transactional
    public void handleBookingCanceledEvent(BookingCanceledEvent event) {
        String subject = "Booking Canceled";
        String body = htmlBodyService.buildBookingCanceledEmailBody(event);

        EmailNotification notification = mapper
                .fromBookingCanceledEventToEmailNotification(event, subject, body);

        repository.save(notification);

        try {
            emailService.sendHtmlEmail(event.email(), subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (MessagingException ex) {
            log.error("Failed to cancel booking {}", event.bookingId(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        repository.save(notification);
    }


    @Transactional
    public void handleBookingExpiredEvent(BookingExpiredEvent event) {
        String subject = "Booking Expired";
        String body = htmlBodyService.buildBookingExpiredEmailBody(event);

        EmailNotification notification = mapper
                .fromBookingExpiredEventToEmailNotification(event, subject, body);

        repository.save(notification);

        try {
            emailService.sendHtmlEmail(event.email(), subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (MessagingException ex) {
            log.error("Failed to expire booking {}", event.bookingId(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        repository.save(notification);

    }



}


