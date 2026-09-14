package com.sorokaandriy.booking_service.client;

import com.sorokaandriy.booking_service.dto.requests.ConfirmSeatsRequest;
import com.sorokaandriy.booking_service.dto.requests.HeldSeatRequest;
import com.sorokaandriy.booking_service.dto.requests.ReleaseSeatsRequest;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
public class EventClient {

    private final RestClient restClient;
    private final String eventUrl;

    public EventClient(@Value("${service.event.api-url}") String eventUrl){
        this.eventUrl = eventUrl;
        this.restClient = RestClient.create();
    }


    public HeldSeatsResponse holdSeat(UUID eventId, HeldSeatRequest request) {
        return restClient.post()
                .uri(eventUrl + "/{eventId}/hold-seats", eventId)
                .headers(this::forwardUserHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(HeldSeatsResponse.class);
    }


    public ResponseEntity<Void> releaseSeats(UUID eventId, ReleaseSeatsRequest request){
        return restClient.post()
                .uri(eventUrl + "/{eventId}/release-seats", eventId)
                .headers(this::forwardUserHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }


    public ResponseEntity<Void> confirmSeats(UUID eventId, ConfirmSeatsRequest request){
        return restClient.post()
                .uri(eventUrl + "/{eventId}/confirm-seats", eventId)
                .headers(this::forwardUserHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void forwardUserHeaders(org.springframework.http.HttpHeaders headers) {
        HttpServletRequest currentRequest = getCurrentRequest();
        if (currentRequest != null) {
            copyHeader(currentRequest, headers, "X-User-Id");
            copyHeader(currentRequest, headers, "X-User-Email");
            copyHeader(currentRequest, headers, "X-User-Role");
        } else {
            headers.set("X-User-Id", "00000000-0000-0000-0000-000000000000");
            headers.set("X-User-Role", "SYSTEM");
        }
    }

    private void copyHeader(HttpServletRequest request, org.springframework.http.HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
