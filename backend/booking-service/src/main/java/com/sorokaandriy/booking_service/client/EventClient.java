package com.sorokaandriy.booking_service.client;

import com.sorokaandriy.booking_service.dto.requests.ConfirmSeatsRequest;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.booking_service.dto.requests.HeldSeatRequest;
import com.sorokaandriy.booking_service.dto.requests.ReleaseSeatsRequest;
import com.sorokaandriy.booking_service.dto.responses.HeldSeatsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
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
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(HeldSeatsResponse.class);
    }


    public ResponseEntity<Void> releaseSeats(UUID eventId, ReleaseSeatsRequest request){
        return restClient.post()
                .uri(eventUrl + "/{eventId}/release-seats", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }


    public ResponseEntity<Void> confirmSeats(UUID eventId, ConfirmSeatsRequest request){
        return restClient.post()
                .uri(eventUrl + "/{eventId}/confirm-seats", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
