package com.sorokaandriy.booking_service.dto.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorValidationResponse {

    private int status;
    private String message;
    private Map<String, String> errors;
    private Instant instant;

    public ErrorValidationResponse(Map<String, String> errors) {
        this.status = 400;
        this.message = "Validation failed";
        this.errors = errors;
        this.instant = Instant.now();
    }
}
