package com.sorokaandriy.booking_service.service;

import com.sorokaandriy.booking_service.entity.Booking;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import com.sorokaandriy.booking_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingService service;
    private final BookingRepository repository;


    @Scheduled(fixedDelay = 2000)
    public void expirePendingBookings(){
        List<Booking> expired  = repository.findAllByStatusAndExpiresAtBefore(BookingStatus.PENDING,
                Instant.now());

        log.info("Found {} expired pending bookings", expired.size());

        for (Booking booking : expired) {
            try {
                service.expireBooking(booking);
            } catch (Exception ex) {
                log.error("Failed to expire booking {}", booking.getId(), ex);
            }
        }
    }
}
