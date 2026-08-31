package com.sorokaandriy.event_service.controller;


import com.sorokaandriy.event_service.dto.requests.VenueRequest;
import com.sorokaandriy.event_service.dto.responses.VenueResponse;
import com.sorokaandriy.event_service.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService service;

    @PostMapping
    public ResponseEntity<VenueResponse> createVenue(
            @Valid @RequestBody VenueRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createVenue(request));
    }

    @GetMapping
    public ResponseEntity<Page<VenueResponse>> findAllVenue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        return ResponseEntity.ok(service.findAllVenues(page, size, sortBy));
    }


    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> findVenue(
            @PathVariable UUID id
            ){
        return ResponseEntity.ok(service.findVenue(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<VenueResponse> updateVenue(
            @PathVariable UUID id,
            @Valid @RequestBody VenueRequest request
    ){
        return ResponseEntity.ok(service.updateVenue(id,request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(
            @PathVariable UUID id
    ){
        service.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }
}
