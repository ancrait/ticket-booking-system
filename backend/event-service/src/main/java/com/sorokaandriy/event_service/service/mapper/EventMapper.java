package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.requests.EventRequest;
import com.sorokaandriy.event_service.dto.responses.EventResponse;
import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventMapper {

    public Event fromEventRequestToEvent(EventRequest request, UUID organizerId, Hall hall) {

        return Event.builder()
                .title(request.title())
                .description(request.description())
                .posterUrl(request.posterUrl())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .status(EventStatus.DRAFT)
                .organizerId(organizerId)
                .hall(hall)
                .build();
    }

    public EventResponse fromEventToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .posterUrl(event.getPosterUrl())
                .startsAt(event.getStartsAt())
                .endsAt(event.getEndsAt())
                .status(event.getStatus())
                .organizerId(event.getOrganizerId())
                .hallId(event.getHall().getId())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
