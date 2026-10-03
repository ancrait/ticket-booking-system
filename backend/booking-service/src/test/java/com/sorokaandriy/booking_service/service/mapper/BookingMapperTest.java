package com.sorokaandriy.booking_service.service.mapper;

import com.sorokaandriy.booking_service.dto.event.BookingCanceledEvent;
import com.sorokaandriy.booking_service.dto.event.BookingCreatedEvent;
import com.sorokaandriy.booking_service.dto.event.BookingExpiredEvent;
import com.sorokaandriy.booking_service.dto.requests.BookingItemsRequest;
import com.sorokaandriy.booking_service.dto.requests.CreateBookingRequest;
import com.sorokaandriy.booking_service.dto.responses.BookingResponse;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.booking_service.entity.Booking;
import com.sorokaandriy.booking_service.entity.BookingItem;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingMapperTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SEAT_1 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SEAT_2 = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String EMAIL = "user@example.com";
    private static final String EVENT_TITLE = "Rock Concert";

    private final BookingMapper mapper = new BookingMapper();

    private Booking booking() {
        return Booking.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .eventId(EVENT_ID)
                .eventTitle(EVENT_TITLE)
                .status(BookingStatus.PENDING)
                .totalPrice(new BigDecimal("300.00"))
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T10:10:00Z"))
                .build();
    }

    @Test
    void fromBookingToBookingResponse_mapsAllFields() {
        BookingResponse response = mapper.fromBookingToBookingResponse(booking());

        assertThat(response.id()).isEqualTo(BOOKING_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.eventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-01-01T10:10:00Z"));
    }

    @Test
    void fromHeldSeatInfoToBookingItem_mapsSeatFields() {
        List<HeldSeatInfo> heldSeats = List.of(
                HeldSeatInfo.builder()
                        .eventSeatId(SEAT_1)
                        .rowNumber(5)
                        .seatNumber(10)
                        .sector("A")
                        .price(new BigDecimal("100.00"))
                        .build(),
                HeldSeatInfo.builder()
                        .eventSeatId(SEAT_2)
                        .rowNumber(5)
                        .seatNumber(11)
                        .sector("B")
                        .price(new BigDecimal("200.00"))
                        .build());

        List<BookingItem> items = mapper.fromHeldSeatInfoToBookingItem(heldSeats);

        assertThat(items).hasSize(2);
        BookingItem first = items.getFirst();
        assertThat(first.getEventSeatId()).isEqualTo(SEAT_1);
        assertThat(first.getRowNumber()).isEqualTo(5);
        assertThat(first.getSeatNumber()).isEqualTo(10);
        assertThat(first.getPrice()).isEqualByComparingTo("100.00");
        assertThat(first.getBooking()).isNull();
        assertThat(items.get(1).getEventSeatId()).isEqualTo(SEAT_2);
        assertThat(items.get(1).getSeatNumber()).isEqualTo(11);
        assertThat(items.get(1).getPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void createBooking_buildsPendingBookingExpiringInTenMinutes() {
        CreateBookingRequest request = new CreateBookingRequest(
                EVENT_ID, List.of(new BookingItemsRequest(SEAT_1)));

        Booking booking = mapper.createBooking(
                USER_ID, EMAIL, request, EVENT_TITLE, new BigDecimal("300.00"));

        assertThat(booking.getUserId()).isEqualTo(USER_ID);
        assertThat(booking.getEmail()).isEqualTo(EMAIL);
        assertThat(booking.getEventId()).isEqualTo(EVENT_ID);
        assertThat(booking.getEventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("300.00");
        assertThat(booking.getExpiresAt()).isAfter(Instant.now().plus(9, ChronoUnit.MINUTES));
        assertThat(booking.getExpiresAt()).isBefore(Instant.now().plus(11, ChronoUnit.MINUTES));
    }

    @Test
    void bookingCreatedEvent_mapsAllFieldsIncludingSeatIds() {
        Booking booking = booking();

        BookingCreatedEvent event = mapper.bookingCreatedEvent(
                booking, List.of(SEAT_1, SEAT_2));

        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.eventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(event.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(event.seatIds()).containsExactly(SEAT_1, SEAT_2);
    }

    @Test
    void fromBookingToBookingCanceledEvent_mapsFieldsWithFixedReason() {
        Booking booking = booking();

        BookingCanceledEvent event = mapper.fromBookingToBookingCanceledEvent(USER_ID, booking);

        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.reason()).isEqualTo("Booking canceled");
    }

    @Test
    void fromBookingToBookingExpiredEvent_mapsFieldsWithFixedReason() {
        Booking booking = booking();

        BookingExpiredEvent event = mapper.fromBookingToBookingExpiredEvent(booking);

        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.reason()).isEqualTo("booking-expired");
    }
}
