package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.responses.EventSeatResponse;
import com.sorokaandriy.event_service.service.EventSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/seats")
@RequiredArgsConstructor
public class EventSeatController {

    private final EventSeatService service;


    @GetMapping
    public ResponseEntity<Page<EventSeatResponse>> findAllEventSeats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(service
                .findAllEventSeats(page, size, sortBy, eventId));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<EventSeatResponse>> findAvailableEventSeats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @PathVariable UUID eventId
    ){
        return ResponseEntity.ok(service
                .findAvailableEventSeats(page, size, sortBy,eventId));
    }






}
