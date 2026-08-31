package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.requests.VenueRequest;
import com.sorokaandriy.event_service.dto.responses.VenueResponse;
import com.sorokaandriy.event_service.entity.Venue;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class VenueMapper {


    public Venue fromVenueRequestToVenue(VenueRequest request) {

        return Venue.builder()
                .name(request.name())
                .city(request.city())
                .address(request.address())
                .createdAt(Instant.now())
                .build();
    }

    public VenueResponse fromVenueToVenueResponse(Venue venue) {

        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .city(venue.getCity())
                .address(venue.getAddress())
                .createdAt(venue.getCreatedAt())
                .build();
    }
}
