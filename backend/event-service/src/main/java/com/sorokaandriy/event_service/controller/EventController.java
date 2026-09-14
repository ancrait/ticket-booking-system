package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.EventRequest;
import com.sorokaandriy.event_service.dto.responses.EventResponse;
import com.sorokaandriy.event_service.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;


    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            @RequestHeader("X-User-Id") String organizerId
            ){
        return ResponseEntity.ok(service.createEvent(request, UUID.fromString(organizerId)));
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> findAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        return ResponseEntity.ok(service.findAllEvent(page, size, sortBy));
    }

    @GetMapping("/sortedBy")
    public ResponseEntity<Page<EventResponse>> findPublishedEventByCityAndDate(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        return ResponseEntity.ok(service
                .findPublishedEventByCityAndDate(city, date, page, size, sortBy));
    }

    @GetMapping("/by-venue/{venueId}")
    public ResponseEntity<Page<EventResponse>> findPublishedEventsByVenue(
            @PathVariable UUID venueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        return ResponseEntity.ok(service.findPublishedEventsByVenue(venueId, page, size, sortBy));
    }


    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findEventById(
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(service.findEventById(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request
    ){
        return ResponseEntity.ok(service.updateEvent(id, request));
    }


    @PostMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable UUID id){
        return ResponseEntity.ok(service.publishEvent(id));

    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelEvent(
            @PathVariable UUID id){
        service.cancelEvent(id);
        return ResponseEntity.noContent().build();

    }

    @GetMapping("/organizer/my")
    public ResponseEntity<List<EventResponse>> findMyEvents(
            @RequestHeader("X-User-Id") String organizerId
    ) {
        return ResponseEntity.ok(service.findByOrganizerId(UUID.fromString(organizerId)));
    }




}
