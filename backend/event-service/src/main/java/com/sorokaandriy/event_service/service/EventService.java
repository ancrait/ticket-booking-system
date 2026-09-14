package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.EventRequest;
import com.sorokaandriy.event_service.dto.responses.EventResponse;
import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.EventSeat;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Seat;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import com.sorokaandriy.event_service.exception.EventNotFoundException;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.repository.EventRepository;
import com.sorokaandriy.event_service.repository.EventSeatRepository;
import com.sorokaandriy.event_service.repository.HallRepository;
import com.sorokaandriy.event_service.service.mapper.EventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final HallRepository hallRepository;
    private final EventMapper eventMapper;
    private final EventSeatRepository eventSeatRepository;

    @Value("${event.prices.vip}")
    private BigDecimal vipPrice;
    @Value("${event.prices.a}")
    private BigDecimal sectorAPrice;
    @Value("${event.prices.b}")
    private BigDecimal sectorBPrice;
    @Value("${event.prices.default}")
    private BigDecimal defaultPrice;


    @Transactional
    public EventResponse createEvent(EventRequest request, UUID organizerId) {

        Hall hall = hallRepository.findById(request.hallId())
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + request.hallId() + " not found"));
        log.info("Get hall from db {}", request.hallId());
        Event event = eventMapper.fromEventRequestToEvent(request,organizerId, hall);

        return eventMapper.fromEventToEventResponse(eventRepository.save(event));
    }


    public Page<EventResponse> findAllEvent(int page, int size, String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        return eventRepository.findAll(pageable)
                .map(event -> eventMapper.fromEventToEventResponse(event));

    }


    public EventResponse findEventById(UUID id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + id + " not found"));
        log.info("Get event from db {}", event.getId());

        return eventMapper.fromEventToEventResponse(event);
    }


    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request) {

        Hall hall = hallRepository.findById(request.hallId())
                .orElseThrow(() -> new HallNotFoundException("Hall with id " + request.hallId() + " not found"));
        log.info("Get hall from db {}", request.hallId());

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + id + " not found"));
        log.info("Get event from db {}", event.getId());

        updateEvent(event, request, hall);
        eventRepository.save(event);

        return eventMapper.fromEventToEventResponse(event);


    }

    private void updateEvent(Event event, EventRequest request, Hall hall){
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setPosterUrl(request.posterUrl());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setHall(hall);
    }


    @Transactional
    public EventResponse publishEvent(UUID id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + id + " not found"));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException("Only draft event can be published");
        }

        if (eventSeatRepository.existsByEventId(id)) {
            throw new SeatsAlreadyGeneratedException("Event seats already generated for event " + id);
        }

       List<Seat> seats = event.getHall().getSeats();

        List<EventSeat> eventSeats = seats.stream()
                .map(seat -> EventSeat.builder()
                        .event(event)
                        .seat(seat)
                        .price(determinePrice(seat.getSector()))
                        .status(EventSeatStatus.FREE)
                        .build())
                .toList();

        eventSeatRepository.saveAll(eventSeats);
        event.setStatus(EventStatus.PUBLISHED);

        return eventMapper.fromEventToEventResponse(event);
    }



    private BigDecimal determinePrice(String sector) {
        return switch (sector.toUpperCase()) {
            case "VIP" -> vipPrice;
            case "A" -> sectorAPrice;
            case "B" -> sectorBPrice;
            default -> defaultPrice;
        };
    }


    @Transactional
    public void cancelEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Only published event can be cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
    }


    public List<EventResponse> findByOrganizerId(UUID uuid) {

        List<Event> events = eventRepository.findByOrganizerId(uuid);

        return events.stream().map(event ->
                eventMapper.fromEventToEventResponse(event)).toList();
    }


    public Page<EventResponse> findPublishedEventByCityAndDate(String city, LocalDate date,
                                                               int page, int size, String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        return eventRepository.findPublishedEvents(EventStatus.PUBLISHED, city, date, pageable)
                .map(event -> eventMapper.fromEventToEventResponse(event));

    }

    public Page<EventResponse> findPublishedEventsByVenue(UUID venueId, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return eventRepository.findPublishedEventsByVenueId(EventStatus.PUBLISHED, venueId, pageable)
                .map(event -> eventMapper.fromEventToEventResponse(event));
    }
}
