package com.sorokaandriy.booking_service.service.mapper;

import com.sorokaandriy.booking_service.dto.event.BookingCanceledEvent;
import com.sorokaandriy.booking_service.dto.event.BookingExpiredEvent;
import com.sorokaandriy.booking_service.dto.requests.CreateBookingRequest;
import com.sorokaandriy.booking_service.dto.responses.BookingResponse;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.booking_service.entity.Booking;
import com.sorokaandriy.booking_service.entity.BookingItem;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import com.sorokaandriy.booking_service.dto.event.BookingCreatedEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BookingMapper {


    public BookingResponse fromBookingToBookingResponse(Booking saved) {

        return BookingResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .email(saved.getEmail())
                .eventId(saved.getEventId())
                .eventTitle(saved.getEventTitle())
                .status(saved.getStatus())
                .totalPrice(saved.getTotalPrice())
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    public List<BookingItem> fromHeldSeatInfoToBookingItem(
            List<HeldSeatInfo> heldSeats){
        return heldSeats.stream()
               .map(heldSeatInfo ->
                       BookingItem.builder()
                               .eventSeatId(heldSeatInfo.eventSeatId())
                               .rowNumber(heldSeatInfo.rowNumber())
                               .seatNumber(heldSeatInfo.seatNumber())
                               .price(heldSeatInfo.price())
                               .build()).toList();
    }


    public Booking createBooking(UUID userId, String email,
                                 CreateBookingRequest request,
                                 String eventTitle, BigDecimal totalPrice){

        return Booking.builder()
                .userId(userId)
                .email(email)
                .eventId(request.eventId())
                .eventTitle(eventTitle)
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
    }


    public BookingCreatedEvent bookingCreatedEvent(Booking saved, List<UUID> seatsId){

        return BookingCreatedEvent
                .builder()
                .bookingId(saved.getId())
                .userId(saved.getUserId())
                .email(saved.getEmail())
                .eventId(saved.getEventId())
                .eventTitle(saved.getEventTitle())
                .totalPrice(saved.getTotalPrice())
                .seatIds(seatsId)
                .build();
    }


    public BookingCanceledEvent fromBookingToBookingCanceledEvent(UUID userId, Booking booking) {
        return BookingCanceledEvent.builder()
                .bookingId(booking.getId())
                .userId(userId)
                .email(booking.getEmail())
                .eventId(booking.getEventId())
                .reason("Booking canceled")
                .build();
    }

    public BookingExpiredEvent fromBookingToBookingExpiredEvent(Booking booking) {

        return BookingExpiredEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .email(booking.getEmail())
                .eventId(booking.getEventId())
                .reason("booking-expired")
                .build();
    }
}
