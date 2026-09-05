package com.sorokaandriy.booking_service.exception;

public class BookingCreationException extends RuntimeException {
    public BookingCreationException(String message) {
        super(message);
    }

    public BookingCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
