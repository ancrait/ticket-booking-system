package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.ConfirmSeatsRequest;
import com.sorokaandriy.event_service.dto.requests.HeldSeatRequest;
import com.sorokaandriy.event_service.dto.requests.ReleaseSeatsRequest;
import com.sorokaandriy.event_service.dto.responses.HeldSeatsResponse;
import com.sorokaandriy.event_service.service.EventSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventSeatService service;

    @PostMapping("/{eventId}/hold-seats")
    public ResponseEntity<HeldSeatsResponse> holdSeats(
            @PathVariable UUID eventId,
            @Valid @RequestBody HeldSeatRequest request
    ){
        return ResponseEntity.ok(service.holdSeats(eventId, request));
    }


    @PostMapping("/{eventId}/release-seats")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable UUID eventId,
            @Valid @RequestBody ReleaseSeatsRequest request
            ){
        service.releaseSeats(eventId, request);
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/{eventId}/confirm-seats")
    public ResponseEntity<Void> confirmSeats(
            @PathVariable UUID eventId,
            @Valid @RequestBody ConfirmSeatsRequest request
    ){
        service.confirmSeats(eventId, request);
        return ResponseEntity.noContent().build();
    }


}
