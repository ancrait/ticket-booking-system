package com.sorokaandriy.payment_service.client;

import com.sorokaandriy.payment_service.client.dto.BookingResponse;
import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingClient {

    private final RestClient bookingRestClient;

    public BookingResponse getBooking(UUID bookingId, UUID userId, String role) {
        try {
            return bookingRestClient.get()
                    .uri("/{bookingId}", bookingId)
                    .header("X-User-Id", userId.toString())
                    .header("X-User-Role", role)
                    .retrieve()
                    .body(BookingResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("Failed to fetch booking {} from booking-service: {} - {}",
                    bookingId, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new PaymentProcessingException("Failed to fetch booking: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while fetching booking {} from booking-service", bookingId, ex);
            throw new PaymentProcessingException("Unexpected error while fetching booking: " + ex.getMessage());
        }
    }
}
