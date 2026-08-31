package com.sorokaandriy.event_service.exception;

public class SeatsAlreadyGeneratedException extends RuntimeException {
    public SeatsAlreadyGeneratedException(String message) {
        super(message);
    }
}
