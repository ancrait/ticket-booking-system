package com.sorokaandriy.booking_service.service;

import com.sorokaandriy.booking_service.client.EventClient;
import com.sorokaandriy.booking_service.dto.event.*;
import com.sorokaandriy.booking_service.dto.requests.ConfirmSeatsRequest;
import com.sorokaandriy.booking_service.dto.requests.CreateBookingRequest;
import com.sorokaandriy.booking_service.dto.requests.HeldSeatRequest;
import com.sorokaandriy.booking_service.dto.requests.ReleaseSeatsRequest;
import com.sorokaandriy.booking_service.dto.responses.BookingResponse;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatsResponse;
import com.sorokaandriy.booking_service.entity.Booking;
import com.sorokaandriy.booking_service.entity.BookingItem;
import com.sorokaandriy.booking_service.entity.OutBox;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import com.sorokaandriy.booking_service.exception.BookingNotFoundException;
import com.sorokaandriy.booking_service.exception.BookingCreationException;
import com.sorokaandriy.booking_service.exception.SeatHoldException;
import com.sorokaandriy.booking_service.repository.BookingRepository;
import com.sorokaandriy.booking_service.repository.OutBoxRepository;
import com.sorokaandriy.booking_service.service.mapper.BookingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OutBoxRepository outBoxRepository;
    private final BookingMapper mapper;
    private final EventClient client;
    private final ObjectMapper objectMapper;


    @Transactional
    public BookingResponse createBooking(UUID userId, String email, CreateBookingRequest request) {

        List<UUID> seatsId = request.bookingItemsRequests()
                .stream().map(bookingItemsRequest -> bookingItemsRequest.eventSeatId())
                .toList();

        HeldSeatsResponse heldSeatsResponse;
        try {
            heldSeatsResponse = client.holdSeat(request.eventId(), new HeldSeatRequest(seatsId));
        } catch (Exception ex) {
            log.error("Failed to hold seats for event {}: {}", request.eventId(), ex.getMessage(), ex);
            throw new SeatHoldException("Unable to hold selected seats", ex);
        }

        String eventTitle = heldSeatsResponse.eventTitle();

        List<HeldSeatInfo> heldSeats = heldSeatsResponse.seats();

        List<BookingItem> items = mapper.fromHeldSeatInfoToBookingItem(heldSeats);


        BigDecimal totalPrice = items.stream()
                .map(BookingItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        Booking booking = mapper.createBooking(userId, email, request,
                eventTitle, totalPrice);


        items.forEach(item -> item.setBooking(booking));

        booking.setBookingItems(items);

        try {
            Booking saved = bookingRepository.save(booking);

            BookingCreatedEvent event = mapper.bookingCreatedEvent(saved, seatsId);

            outBoxRepository.save(OutBox.builder()
                    .aggregateId(String.valueOf(saved.getId()))
                    .topic("booking-created")
                    .payload(serialize(event))
                    .build());

            return mapper.fromBookingToBookingResponse(saved);

        } catch (Exception e) {
            client.releaseSeats(request.eventId(), new ReleaseSeatsRequest(seatsId));
            throw new BookingCreationException("Failed to create booking", e);
        }

    }


    public BookingResponse findBookingById(UUID id, UUID userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking with id " + id + " not found"));

        if (!booking.getUserId().equals(userId)){
            throw new AccessDeniedException("Not your booking");
        }

        return mapper.fromBookingToBookingResponse(booking);
    }


    public Page<BookingResponse> findMyBookings(UUID userId, int page, int size,
                                                String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        return bookingRepository.findAllByUserId(userId, pageable)
                .map(booking -> mapper.fromBookingToBookingResponse(booking));

    }


    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new BookingCreationException("Failed to serialize event", ex);
        }
    }

    @Transactional
    public BookingResponse cancelBooking(UUID userId, UUID id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking with id" +
                        " " + id + " not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not your booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.PAID) {
            throw new IllegalStateException("Booking cannot be canceled");
        }

        List<UUID> seatsId = booking.getBookingItems()
                .stream().map(bookingItem -> bookingItem.getEventSeatId()).toList();

        client.releaseSeats(booking.getEventId(), new ReleaseSeatsRequest(seatsId));

        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(Instant.now());

        BookingCanceledEvent canceledEvent = mapper.fromBookingToBookingCanceledEvent(userId,booking);

        outBoxRepository.save(OutBox.builder()
                .aggregateId(booking.getId().toString())
                .topic("booking-canceled")
                .payload(serialize(canceledEvent))
                .build());

        return mapper.fromBookingToBookingResponse(booking);




    }

    @Transactional
    public void markBookingAsPaid(PaymentSuccessEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking with id " + event.bookingId() + " not found"));


        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }

        List<UUID> seatsId = booking.getBookingItems()
                        .stream().map(bookingItem -> bookingItem.getEventSeatId()).toList();

        client.confirmSeats(booking.getEventId(), new ConfirmSeatsRequest(seatsId));

        booking.setStatus(BookingStatus.PAID);
        booking.setUpdatedAt(Instant.now());

    }


    @Transactional
    public void markBookingAsCanceled(PaymentFailedEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking with id " + event.bookingId() + " not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }

        List<UUID> seatsId = booking.getBookingItems()
                .stream().map(bookingItem -> bookingItem.getEventSeatId()).toList();

        client.releaseSeats(booking.getEventId(), new ReleaseSeatsRequest(seatsId));

        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(Instant.now());


    }


    @Transactional
    public void expireBooking(Booking booking) {

        Booking managedBooking = bookingRepository.findById(booking.getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking with id " + booking.getId() + " not found"));

        if (managedBooking.getStatus() != BookingStatus.PENDING){
            return;
        }

        List<UUID> seatsId = managedBooking.getBookingItems()
                .stream().map(bookingItem -> bookingItem.getEventSeatId()).toList();

        client.releaseSeats(managedBooking.getEventId(), new ReleaseSeatsRequest(seatsId));

        managedBooking.setStatus(BookingStatus.EXPIRED);
        managedBooking.setUpdatedAt(Instant.now());


        BookingExpiredEvent expiredEvent = mapper.fromBookingToBookingExpiredEvent(managedBooking);


        outBoxRepository.save(OutBox.builder()
                .aggregateId(managedBooking.getId().toString())
                .topic("booking-expired")
                .payload(serialize(expiredEvent))
                .build());


    }
}
