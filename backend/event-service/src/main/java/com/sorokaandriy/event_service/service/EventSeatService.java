package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.ConfirmSeatsRequest;
import com.sorokaandriy.event_service.dto.requests.HeldSeatRequest;
import com.sorokaandriy.event_service.dto.requests.ReleaseSeatsRequest;
import com.sorokaandriy.event_service.dto.responses.EventSeatResponse;
import com.sorokaandriy.event_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.event_service.dto.responses.HeldSeatsResponse;
import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.EventSeat;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import com.sorokaandriy.event_service.exception.EventNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsNotAvailableException;
import com.sorokaandriy.event_service.repository.EventRepository;
import com.sorokaandriy.event_service.repository.EventSeatRepository;
import com.sorokaandriy.event_service.service.mapper.EventSeatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSeatService {

    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final EventSeatMapper mapper;


    @Transactional(readOnly = true)
    public Page<EventSeatResponse> findAllEventSeats(int page, int size,
                                                     String sortBy, UUID eventId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        return eventSeatRepository.findByEventId(eventId, pageable)
                .map(eventSeat -> mapper.fromEventSeatToEventSeatResponse(eventSeat));
    }


    @Transactional(readOnly = true)
    public Page<EventSeatResponse> findAvailableEventSeats(int page, int size,
                                                                      String sortBy, UUID eventId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        return eventSeatRepository.findEventSeatByEventIdAndStatus(
                eventId, EventSeatStatus.FREE, pageable)
                .map(eventSeat -> mapper.fromEventSeatToEventSeatResponse(eventSeat));

    }


    @Transactional
    public HeldSeatsResponse holdSeats(UUID eventId, HeldSeatRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));

        List<EventSeat> eventSeats = eventSeatRepository.findAllByEventIdAndIdInAndStatus
                (eventId, request.seatIds(), EventSeatStatus.FREE);

        if (eventSeats.size() != request.seatIds().size()){
            throw new SeatsNotAvailableException("Some seats are already taken");
        }

        eventSeats.forEach(seat -> seat.setStatus(EventSeatStatus.HELD));

        List<HeldSeatInfo> heldSeatInfos = eventSeats.stream().
                map(eventSeat -> mapper.fromEventSeatToHeldSeatInfo(eventSeat)).toList();

        return HeldSeatsResponse.builder()
                .eventTitle(event.getTitle())
                .seats(heldSeatInfos)
                .build();
    }


    @Transactional
    public void releaseSeats(UUID eventId, ReleaseSeatsRequest request) {

        List<EventSeat> eventSeats = eventSeatRepository.findAllByEventIdAndIdIn(eventId, request.seatIds());

        if (eventSeats.size() != request.seatIds().size()) {
            throw new SeatsNotAvailableException("Some seats not found");
        }

        for (EventSeat seat : eventSeats) {
            if (seat.getStatus() == EventSeatStatus.HELD) {
                seat.setStatus(EventSeatStatus.FREE);
            } else if (seat.getStatus() == EventSeatStatus.SOLD) {
                throw new SeatsNotAvailableException("Some seats are already sold");
            }
        }

    }


    @Transactional
    public void confirmSeats(UUID eventId, ConfirmSeatsRequest request) {

        List<EventSeat> eventSeats = eventSeatRepository.findAllByEventIdAndIdInAndStatus
                (eventId, request.seatIds(), EventSeatStatus.HELD);

        if (eventSeats.size() != request.seatIds().size()) {
            throw new SeatsNotAvailableException("Some seats are not in HELD status");
        }

        eventSeats.forEach(seat -> seat.setStatus(EventSeatStatus.SOLD));
    }
}
