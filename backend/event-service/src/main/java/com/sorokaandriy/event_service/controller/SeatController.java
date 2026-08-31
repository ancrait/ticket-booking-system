package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.responses.SeatResponse;
import com.sorokaandriy.event_service.service.HallService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/halls/{hallId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final HallService service;

    @PostMapping
    public ResponseEntity<List<SeatResponse>> generateSeats(
            @PathVariable UUID hallId
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.generateSeats(hallId));
    }

    @GetMapping
    public ResponseEntity<Page<SeatResponse>> findSeatsByHallId(
            @PathVariable UUID hallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        return ResponseEntity.ok(service.findSeatsByHallId(hallId, page, size, sortBy));
    }


}
