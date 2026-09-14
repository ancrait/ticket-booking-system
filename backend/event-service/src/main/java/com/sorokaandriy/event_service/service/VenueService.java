package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.VenueRequest;
import com.sorokaandriy.event_service.dto.responses.VenueResponse;
import com.sorokaandriy.event_service.entity.Venue;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import com.sorokaandriy.event_service.repository.VenueRepository;
import com.sorokaandriy.event_service.service.mapper.VenueMapper;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper mapper;


    public VenueResponse createVenue(VenueRequest request) {

        Venue venue = venueRepository.save(mapper.fromVenueRequestToVenue(request));
        log.info("Save venue in db {}", venue.getId());

        return mapper.fromVenueToVenueResponse(venue);
    }


    public VenueResponse findVenue(UUID id) {

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue with id " + id + " not found"));
        log.info("Get venue from db {}", venue.getId());

        return mapper.fromVenueToVenueResponse(venue);
    }

    public Page<VenueResponse> findAllVenues(int page, int size, String sortBy, String city) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        if (city != null && !city.isBlank()) {
            return venueRepository.findByCityIgnoreCase(city, pageable)
                    .map(venue -> mapper.fromVenueToVenueResponse(venue));
        }
        return venueRepository.findAll(pageable).map(venue -> mapper.fromVenueToVenueResponse(venue));
    }


    public VenueResponse updateVenue(UUID id, VenueRequest request) {

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue with id " + id + " not found"));
        log.info("Get venue from db {}", venue.getId());

        venue.setName(request.name());
        venue.setCity(request.city());
        venue.setAddress(request.address());
        venue.setUpdatedAt(Instant.now());
        venueRepository.save(venue);

        log.info("Update venue {}", venue.getId());

        return mapper.fromVenueToVenueResponse(venue);
    }

    public void deleteVenue(UUID id) {

        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue with id " + id + " not found"));
        log.info("Get venue from db {}", venue.getId());

        venueRepository.delete(venue);
    }
}
