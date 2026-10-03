package com.sorokaandriy.booking_service.service;

import com.sorokaandriy.booking_service.entity.Booking;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import com.sorokaandriy.booking_service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private BookingService service;

    @Mock
    private BookingRepository repository;

    private BookingExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BookingExpirationScheduler(service, repository);
    }

    @Test
    void expirePendingBookings_expiresEachExpiredBooking() {
        Booking first = Booking.builder().id(UUID.randomUUID()).build();
        Booking second = Booking.builder().id(UUID.randomUUID()).build();
        when(repository.findAllByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(first, second));

        scheduler.expirePendingBookings();

        verify(service).expireBooking(first);
        verify(service).expireBooking(second);
    }

    @Test
    void expirePendingBookings_continuesWhenOneBookingFailsToExpire() {
        Booking first = Booking.builder().id(UUID.randomUUID()).build();
        Booking second = Booking.builder().id(UUID.randomUUID()).build();
        when(repository.findAllByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("release failed")).when(service).expireBooking(first);

        scheduler.expirePendingBookings();

        verify(service).expireBooking(second);
    }

    @Test
    void expirePendingBookings_doesNothingWhenNoExpiredBookings() {
        when(repository.findAllByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        scheduler.expirePendingBookings();

        verify(service, never()).expireBooking(any());
    }
}
