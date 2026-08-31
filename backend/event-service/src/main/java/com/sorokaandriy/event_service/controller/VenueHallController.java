package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.service.HallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues/{venueId}/halls")
@RequiredArgsConstructor
public class VenueHallController {

    private final HallService service;

    @PostMapping
    public ResponseEntity<HallResponse> createHall(
            @PathVariable UUID venueId,
            @Valid @RequestBody HallRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createHall(venueId, request));
    }

    @GetMapping
    public ResponseEntity<List<HallResponse>> findHallsByVenueId(
            @PathVariable UUID venueId
    ) {
        return ResponseEntity.ok(service.findHallsByVenueId(venueId));
    }
}
