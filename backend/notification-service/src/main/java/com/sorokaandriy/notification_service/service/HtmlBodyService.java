package com.sorokaandriy.notification_service.service;

import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentFailedEvent;
import com.sorokaandriy.notification_service.dto.events.PaymentSuccessEvent;
import org.springframework.stereotype.Service;

@Service
public class HtmlBodyService {

    public String buildRegistrationEmailBody(String firstName, String lastName, String verificationLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Welcome</title>
                </head>
                <body>
                    <h1>Welcome to TIQ, %s %s!</h1>
                    <p>Thank you for registering. Please confirm your email to continue.</p>
                    <p><a href="%s">Verify email</a></p>
                    <p>If you did not create this account, please ignore this email.</p>
                </body>
                </html>
                """.formatted(firstName, lastName, verificationLink);
    }


    public String buildPaymentSuccessEmailBody(PaymentSuccessEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Payment Confirmation</title>
                </head>
                <body>
                    <h1>Payment Successful</h1>
                    <p>Thank you for your purchase!</p>
                    <ul>
                        <li><strong>Booking ID:</strong> %s</li>
                        <li><strong>Payment ID:</strong> %s</li>
                        <li><strong>Amount:</strong> %s UAH</li>
                        <li><strong>Paid at:</strong> %s</li>
                    </ul>
                    <p>You can view your tickets in your personal account.</p>
                </body>
                </html>
                """.formatted(
                event.bookingId(),
                event.paymentId(),
                event.amount(),
                event.paidAt()
        );
    }


    public String buildPaymentFailedEmailBody(PaymentFailedEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Payment Failed</title>
                </head>
                <body>
                    <h1>Payment Failed</h1>
                    <p>We were unable to process your payment.</p>
                    <ul>
                        <li><strong>Booking ID:</strong> %s</li>
                        <li><strong>Payment ID:</strong> %s</li>
                        <li><strong>Reason:</strong> %s</li>
                        <li><strong>Failed at:</strong> %s</li>
                    </ul>
                    <p>Your booking has been canceled. You can try to book again at any time.</p>
                </body>
                </html>
                """.formatted(
                event.bookingId(),
                event.paymentId(),
                event.reason(),
                event.failedAt()
        );
    }


    public String buildBookingCanceledEmailBody(BookingCanceledEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Booking Canceled</title>
                </head>
                <body>
                    <h1>Booking Canceled</h1>
                    <p>Your booking has been canceled.</p>
                    <ul>
                        <li><strong>Booking ID:</strong> %s</li>
                        <li><strong>Event ID:</strong> %s</li>
                        <li><strong>Reason:</strong> %s</li>
                    </ul>
                    <p>If you have already paid, a refund will be processed shortly.</p>
                </body>
                </html>
                """.formatted(
                event.bookingId(),
                event.eventId(),
                event.reason()
        );
    }


    public String buildBookingExpiredEmailBody(BookingExpiredEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Booking Expired</title>
                </head>
                <body>
                    <h1>Booking Expired</h1>
                    <p>Your booking has expired because the payment was not completed in time.</p>
                    <ul>
                        <li><strong>Booking ID:</strong> %s</li>
                        <li><strong>Event ID:</strong> %s</li>
                        <li><strong>Reason:</strong> %s</li>
                    </ul>
                    <p>You can create a new booking if tickets are still available.</p>
                </body>
                </html>
                """.formatted(
                event.bookingId(),
                event.eventId(),
                event.reason()
        );
    }







}
