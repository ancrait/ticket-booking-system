package com.sorokaandriy.event_service.exception;

public class SeatsNotAvailableException extends RuntimeException {
  public SeatsNotAvailableException(String message) {
    super(message);
  }
}
