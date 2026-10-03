package com.sorokaandriy.booking_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.booking_service.client.EventClient;
import com.sorokaandriy.booking_service.dto.event.BookingCanceledEvent;
import com.sorokaandriy.booking_service.dto.event.BookingCreatedEvent;
import com.sorokaandriy.booking_service.dto.event.BookingExpiredEvent;
import com.sorokaandriy.booking_service.dto.event.PaymentFailedEvent;
import com.sorokaandriy.booking_service.dto.event.PaymentSuccessEvent;
import com.sorokaandriy.booking_service.dto.requests.BookingItemsRequest;
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
import com.sorokaandriy.booking_service.exception.BookingCreationException;
import com.sorokaandriy.booking_service.exception.BookingNotFoundException;
import com.sorokaandriy.booking_service.exception.SeatHoldException;
import com.sorokaandriy.booking_service.repository.BookingRepository;
import com.sorokaandriy.booking_service.repository.OutBoxRepository;
import com.sorokaandriy.booking_service.service.mapper.BookingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SEAT_1 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SEAT_2 = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String EMAIL = "user@example.com";
    private static final String EVENT_TITLE = "Rock Concert";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private OutBoxRepository outBoxRepository;

    @Mock
    private EventClient client;

    private ObjectMapper objectMapper;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        bookingService = new BookingService(
                bookingRepository,
                outBoxRepository,
                new BookingMapper(),
                client,
                objectMapper);
    }

    private CreateBookingRequest createBookingRequest() {
        return new CreateBookingRequest(EVENT_ID, List.of(
                new BookingItemsRequest(SEAT_1),
                new BookingItemsRequest(SEAT_2)));
    }

    private HeldSeatsResponse heldSeatsResponse() {
        return HeldSeatsResponse.builder()
                .eventTitle(EVENT_TITLE)
                .seats(List.of(
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
                                .sector("A")
                                .price(new BigDecimal("200.00"))
                                .build()))
                .build();
    }

    private Booking booking(BookingStatus status) {
        Booking booking = Booking.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .email(EMAIL)
                .eventId(EVENT_ID)
                .eventTitle(EVENT_TITLE)
                .status(status)
                .totalPrice(new BigDecimal("300.00"))
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .build();
        List<BookingItem> items = List.of(
                BookingItem.builder()
                        .eventSeatId(SEAT_1)
                        .rowNumber(5)
                        .seatNumber(10)
                        .price(new BigDecimal("100.00"))
                        .build(),
                BookingItem.builder()
                        .eventSeatId(SEAT_2)
                        .rowNumber(5)
                        .seatNumber(11)
                        .price(new BigDecimal("200.00"))
                        .build());
        items.forEach(item -> item.setBooking(booking));
        booking.setBookingItems(items);
        return booking;
    }

    @Test
    void createBooking_savesBookingAndWritesOutboxEvent() throws Exception {
        when(client.holdSeat(EVENT_ID, new HeldSeatRequest(List.of(SEAT_1, SEAT_2))))
                .thenReturn(heldSeatsResponse());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(BOOKING_ID);
            return saved;
        });

        BookingResponse response = bookingService.createBooking(USER_ID, EMAIL, createBookingRequest());

        assertThat(response.id()).isEqualTo(BOOKING_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.eventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.totalPrice()).isEqualByComparingTo("300.00");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getEventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("300.00");
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(9, ChronoUnit.MINUTES));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(11, ChronoUnit.MINUTES));
        assertThat(saved.getBookingItems()).hasSize(2);
        assertThat(saved.getBookingItems()).allSatisfy(
                item -> assertThat(item.getBooking()).isSameAs(saved));
        assertThat(saved.getBookingItems().get(0).getEventSeatId()).isEqualTo(SEAT_1);
        assertThat(saved.getBookingItems().get(0).getRowNumber()).isEqualTo(5);
        assertThat(saved.getBookingItems().get(0).getSeatNumber()).isEqualTo(10);
        assertThat(saved.getBookingItems().get(0).getPrice()).isEqualByComparingTo("100.00");
        assertThat(saved.getBookingItems().get(1).getEventSeatId()).isEqualTo(SEAT_2);
        assertThat(saved.getBookingItems().get(1).getSeatNumber()).isEqualTo(11);
        assertThat(saved.getBookingItems().get(1).getPrice()).isEqualByComparingTo("200.00");

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("booking-created");
        assertThat(outBox.getAggregateId()).isEqualTo(BOOKING_ID.toString());
        BookingCreatedEvent event = objectMapper.readValue(outBox.getPayload(), BookingCreatedEvent.class);
        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.eventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(event.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(event.seatIds()).containsExactly(SEAT_1, SEAT_2);

        verify(client, never()).releaseSeats(any(), any());
        verify(client, never()).confirmSeats(any(), any());
    }

    @Test
    void createBooking_throwsSeatHoldExceptionWhenEventClientFails() {
        when(client.holdSeat(EVENT_ID, new HeldSeatRequest(List.of(SEAT_1, SEAT_2))))
                .thenThrow(new RuntimeException("event-service unavailable"));

        assertThatThrownBy(() -> bookingService.createBooking(USER_ID, EMAIL, createBookingRequest()))
                .isInstanceOf(SeatHoldException.class)
                .hasMessage("Unable to hold selected seats")
                .hasCauseInstanceOf(RuntimeException.class);

        verifyNoInteractions(bookingRepository, outBoxRepository);
        verify(client, never()).releaseSeats(any(), any());
    }

    @Test
    void createBooking_releasesSeatsAndThrowsWhenSaveFails() {
        when(client.holdSeat(EVENT_ID, new HeldSeatRequest(List.of(SEAT_1, SEAT_2))))
                .thenReturn(heldSeatsResponse());
        when(bookingRepository.save(any(Booking.class)))
                .thenThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> bookingService.createBooking(USER_ID, EMAIL, createBookingRequest()))
                .isInstanceOf(BookingCreationException.class)
                .hasMessage("Failed to create booking");

        verify(client).releaseSeats(EVENT_ID, new ReleaseSeatsRequest(List.of(SEAT_1, SEAT_2)));
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void findBookingById_returnsBookingForOwner() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.PAID)));

        BookingResponse response = bookingService.findBookingById(BOOKING_ID, USER_ID);

        assertThat(response.id()).isEqualTo(BOOKING_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.eventTitle()).isEqualTo(EVENT_TITLE);
        assertThat(response.status()).isEqualTo(BookingStatus.PAID);
        assertThat(response.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(response.expiresAt()).isAfter(Instant.now().plus(4, ChronoUnit.MINUTES));
    }

    @Test
    void findBookingById_throwsWhenBookingNotFound() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findBookingById(BOOKING_ID, USER_ID))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking with id " + BOOKING_ID + " not found");
    }

    @Test
    void findBookingById_throwsWhenBookingBelongsToAnotherUser() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.PENDING)));

        assertThatThrownBy(() -> bookingService.findBookingById(BOOKING_ID, OTHER_USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not your booking");
    }

    @Test
    void findMyBookings_returnsPageSortedByRequestedField() {
        when(bookingRepository.findAllByUserId(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking(BookingStatus.PAID)), PageRequest.of(0, 10), 1));

        Page<BookingResponse> result = bookingService.findMyBookings(USER_ID, 0, 10, "createdAt");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(BOOKING_ID);
        assertThat(result.getContent().getFirst().userId()).isEqualTo(USER_ID);
        assertThat(result.getContent().getFirst().status()).isEqualTo(BookingStatus.PAID);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findAllByUserId(any(UUID.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void cancelBooking_releasesSeatsMarksCanceledAndWritesOutboxEvent() throws Exception {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancelBooking(USER_ID, BOOKING_ID);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(booking.getUpdatedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELED);
        assertThat(response.id()).isEqualTo(BOOKING_ID);

        verify(client).releaseSeats(EVENT_ID, new ReleaseSeatsRequest(List.of(SEAT_1, SEAT_2)));

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("booking-canceled");
        assertThat(outBox.getAggregateId()).isEqualTo(BOOKING_ID.toString());
        BookingCanceledEvent event = objectMapper.readValue(outBox.getPayload(), BookingCanceledEvent.class);
        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.reason()).isEqualTo("Booking canceled");
    }

    @Test
    void cancelBooking_cancelsPaidBooking() {
        Booking booking = booking(BookingStatus.PAID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancelBooking(USER_ID, BOOKING_ID);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELED);
        verify(client).releaseSeats(EVENT_ID, new ReleaseSeatsRequest(List.of(SEAT_1, SEAT_2)));
    }

    @Test
    void cancelBooking_throwsWhenBookingNotFound() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(USER_ID, BOOKING_ID))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking with id " + BOOKING_ID + " not found");
    }

    @Test
    void cancelBooking_throwsWhenBookingBelongsToAnotherUser() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.PENDING)));

        assertThatThrownBy(() -> bookingService.cancelBooking(OTHER_USER_ID, BOOKING_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not your booking");

        verify(client, never()).releaseSeats(any(), any());
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void cancelBooking_throwsWhenBookingStatusIsFinal() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.EXPIRED)));

        assertThatThrownBy(() -> bookingService.cancelBooking(USER_ID, BOOKING_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Booking cannot be canceled");

        verify(client, never()).releaseSeats(any(), any());
        verifyNoInteractions(outBoxRepository);
    }

    @Test
    void markBookingAsPaid_confirmsSeatsAndMarksPaid() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        bookingService.markBookingAsPaid(PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId("pay_123")
                .amount(new BigDecimal("300.00"))
                .paidAt(Instant.now())
                .build());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(booking.getUpdatedAt()).isNotNull();
        verify(client).confirmSeats(EVENT_ID, new ConfirmSeatsRequest(List.of(SEAT_1, SEAT_2)));
    }

    @Test
    void markBookingAsPaid_skipsWhenBookingIsNotPending() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.EXPIRED)));

        bookingService.markBookingAsPaid(PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId("pay_123")
                .amount(new BigDecimal("300.00"))
                .paidAt(Instant.now())
                .build());

        verifyNoInteractions(client);
    }

    @Test
    void markBookingAsPaid_throwsWhenBookingNotFound() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.markBookingAsPaid(PaymentSuccessEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId("pay_123")
                .amount(new BigDecimal("300.00"))
                .paidAt(Instant.now())
                .build()))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking with id " + BOOKING_ID + " not found");
    }

    @Test
    void markBookingAsCanceled_releasesSeatsAndMarksCanceled() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        bookingService.markBookingAsCanceled(PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId(UUID.randomUUID())
                .reason("card_declined")
                .failedAt(Instant.now())
                .build());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(booking.getUpdatedAt()).isNotNull();
        verify(client).releaseSeats(EVENT_ID, new ReleaseSeatsRequest(List.of(SEAT_1, SEAT_2)));
    }

    @Test
    void markBookingAsCanceled_skipsWhenBookingIsNotPending() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.PAID)));

        bookingService.markBookingAsCanceled(PaymentFailedEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .paymentId(UUID.randomUUID())
                .reason("card_declined")
                .failedAt(Instant.now())
                .build());

        verifyNoInteractions(client);
    }

    @Test
    void expireBooking_reloadsBookingReleasesSeatsAndWritesOutboxEvent() throws Exception {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        bookingService.expireBooking(Booking.builder().id(BOOKING_ID).build());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(booking.getUpdatedAt()).isNotNull();
        verify(client).releaseSeats(EVENT_ID, new ReleaseSeatsRequest(List.of(SEAT_1, SEAT_2)));

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("booking-expired");
        assertThat(outBox.getAggregateId()).isEqualTo(BOOKING_ID.toString());
        BookingExpiredEvent event = objectMapper.readValue(outBox.getPayload(), BookingExpiredEvent.class);
        assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.email()).isEqualTo(EMAIL);
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.reason()).isEqualTo("booking-expired");
    }

    @Test
    void expireBooking_skipsWhenBookingIsNotPending() {
        when(bookingRepository.findById(BOOKING_ID))
                .thenReturn(Optional.of(booking(BookingStatus.PAID)));

        bookingService.expireBooking(Booking.builder().id(BOOKING_ID).build());

        verifyNoInteractions(client, outBoxRepository);
    }
}
