package com.sorokaandriy.booking_service.exception;

public class SeatHoldException extends RuntimeException {
    public SeatHoldException(String message) {
        super(message);
    }

    public SeatHoldException(String message, Throwable cause) {
        super(message, cause);
    }
}
