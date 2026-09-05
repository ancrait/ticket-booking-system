package com.sorokaandriy.booking_service.controller;

import com.sorokaandriy.booking_service.dto.requests.CreateBookingRequest;
import com.sorokaandriy.booking_service.dto.responses.BookingResponse;
import com.sorokaandriy.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody CreateBookingRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createBooking(UUID.fromString(userId), email, request));
    }


    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> findBookingById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ){
        return ResponseEntity.ok(service.findBookingById(id, userId));
    }


    @GetMapping("/my")
    public ResponseEntity<Page<BookingResponse>> findMyBooking(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ){
        return ResponseEntity.ok(service.findMyBookings(userId, page, size, sortBy));
    }


    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBookingById(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(service.cancelBooking(userId, id));
    }
}
