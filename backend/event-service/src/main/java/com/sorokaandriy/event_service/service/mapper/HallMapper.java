package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.entity.Hall;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HallMapper {

    public Hall fromHallRequestToHall(HallRequest request) {
        return Hall.builder()
                .name(request.name())
                .rowsCount(request.rowsCount())
                .seatsPerRow(request.seatsPerRow())
                .createdAt(Instant.now())
                .build();
    }

    public HallResponse fromHallToHallResponse(Hall hall) {
        return HallResponse.builder()
                .id(hall.getId())
                .name(hall.getName())
                .rowsCount(hall.getRowsCount())
                .seatsPerRow(hall.getSeatsPerRow())
                .venueId(hall.getVenue().getId())
                .build();
    }
}
