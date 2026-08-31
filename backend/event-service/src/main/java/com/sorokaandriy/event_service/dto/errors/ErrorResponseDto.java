package com.sorokaandriy.event_service.dto.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {

    private int status;
    private String message;
    private Instant instant;

    public ErrorResponseDto(int status, String message) {
        this.status = status;
        this.message = message;
        this.instant = Instant.now();
    }
}
