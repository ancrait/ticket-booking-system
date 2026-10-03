package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.EventRequest;
import com.sorokaandriy.event_service.dto.responses.EventResponse;
import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.EventSeat;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Seat;
import com.sorokaandriy.event_service.entity.Venue;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import com.sorokaandriy.event_service.exception.EventNotFoundException;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.repository.EventRepository;
import com.sorokaandriy.event_service.repository.EventSeatRepository;
import com.sorokaandriy.event_service.repository.HallRepository;
import com.sorokaandriy.event_service.service.mapper.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HALL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VENUE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORGANIZER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant STARTS_AT = Instant.parse("2026-05-01T18:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-05-01T21:00:00Z");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Captor
    private ArgumentCaptor<List<EventSeat>> eventSeatsCaptor;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService(eventRepository, hallRepository, new EventMapper(), eventSeatRepository);
        ReflectionTestUtils.setField(service, "vipPrice", new BigDecimal("500.00"));
        ReflectionTestUtils.setField(service, "sectorAPrice", new BigDecimal("350.00"));
        ReflectionTestUtils.setField(service, "sectorBPrice", new BigDecimal("250.00"));
        ReflectionTestUtils.setField(service, "defaultPrice", new BigDecimal("200.00"));
    }

    private EventRequest request() {
        return EventRequest.builder()
                .title("Concert")
                .description("Live show")
                .posterUrl("http://example.com/poster.png")
                .startsAt(STARTS_AT)
                .endsAt(ENDS_AT)
                .hallId(HALL_ID)
                .build();
    }

    private Seat seat(UUID id, int row, int number, String sector) {
        return Seat.builder()
                .id(id)
                .rowNumber(row)
                .seatNumber(number)
                .sector(sector)
                .build();
    }

    private Hall hall(List<Seat> seats) {
        return Hall.builder()
                .id(HALL_ID)
                .name("Hall 1")
                .rowsCount(3)
                .seatsPerRow(2)
                .venue(Venue.builder().id(VENUE_ID).name("Arena").city("Kyiv").address("Main St 1").build())
                .seats(seats)
                .build();
    }

    private Event event(EventStatus status, Hall hall) {
        return Event.builder()
                .id(EVENT_ID)
                .title("Concert")
                .description("Live show")
                .posterUrl("http://example.com/poster.png")
                .startsAt(STARTS_AT)
                .endsAt(ENDS_AT)
                .status(status)
                .organizerId(ORGANIZER_ID)
                .hall(hall)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    void createEvent_savesDraftEventWithHallAndOrganizer() {
        Hall hall = hall(List.of());
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(EVENT_ID);
            return saved;
        });

        EventResponse response = service.createEvent(request(), ORGANIZER_ID);

        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.title()).isEqualTo("Concert");
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.organizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(response.hallId()).isEqualTo(HALL_ID);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getHall()).isSameAs(hall);
        assertThat(captor.getValue().getStartsAt()).isEqualTo(STARTS_AT);
    }

    @Test
    void createEvent_throwsHallNotFoundWhenHallIsMissing() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createEvent(request(), ORGANIZER_ID))
                .isInstanceOf(HallNotFoundException.class)
                .hasMessageContaining(HALL_ID.toString());

        verifyNoInteractions(eventRepository);
    }

    @Test
    void findAllEvent_sortsDescendingByRequestedField() {
        Event event = event(EventStatus.PUBLISHED, hall(List.of()));
        when(eventRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(1, 5), 6));

        Page<EventResponse> page = service.findAllEvent(1, 5, "startsAt");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "startsAt"));

        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Concert");
    }

    @Test
    void findEventById_returnsMappedEvent() {
        when(eventRepository.findById(EVENT_ID))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, hall(List.of()))));

        EventResponse response = service.findEventById(EVENT_ID);

        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(response.hallId()).isEqualTo(HALL_ID);
    }

    @Test
    void findEventById_throwsEventNotFoundWhenEventIsMissing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findEventById(EVENT_ID))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(EVENT_ID.toString());
    }

    @Test
    void updateEvent_overwritesFieldsAndHall() {
        Hall hall = hall(List.of());
        Event existing = event(EventStatus.DRAFT, hall);
        Instant newStartsAt = Instant.parse("2026-06-01T18:00:00Z");
        EventRequest request = EventRequest.builder()
                .title("Updated concert")
                .description("Updated description")
                .posterUrl("http://example.com/new.png")
                .startsAt(newStartsAt)
                .endsAt(ENDS_AT)
                .hallId(HALL_ID)
                .build();
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        EventResponse response = service.updateEvent(EVENT_ID, request);

        assertThat(response.title()).isEqualTo("Updated concert");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.posterUrl()).isEqualTo("http://example.com/new.png");
        assertThat(response.startsAt()).isEqualTo(newStartsAt);
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository).save(existing);
    }

    @Test
    void updateEvent_throwsHallNotFoundBeforeTouchingEventRepository() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEvent(EVENT_ID, request()))
                .isInstanceOf(HallNotFoundException.class);

        verifyNoInteractions(eventRepository);
    }

    @Test
    void updateEvent_throwsEventNotFoundWhenEventIsMissing() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall(List.of())));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEvent(EVENT_ID, request()))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(EVENT_ID.toString());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void publishEvent_generatesFreeSeatsPricedBySectorAndPublishes() {
        List<Seat> seats = List.of(
                seat(UUID.randomUUID(), 1, 1, "vip"),
                seat(UUID.randomUUID(), 1, 2, "A"),
                seat(UUID.randomUUID(), 2, 1, "b"),
                seat(UUID.randomUUID(), 2, 2, "C"));
        Event draft = event(EventStatus.DRAFT, hall(seats));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draft));
        when(eventSeatRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(eventSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = service.publishEvent(EVENT_ID);

        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(draft.getStatus()).isEqualTo(EventStatus.PUBLISHED);

        verify(eventSeatRepository).saveAll(eventSeatsCaptor.capture());
        List<EventSeat> saved = eventSeatsCaptor.getValue();
        assertThat(saved).hasSize(4);
        assertThat(saved).allSatisfy(eventSeat -> {
            assertThat(eventSeat.getStatus()).isEqualTo(EventSeatStatus.FREE);
            assertThat(eventSeat.getEvent()).isSameAs(draft);
        });
        assertThat(saved).extracting(EventSeat::getPrice)
                .containsExactly(new BigDecimal("500.00"), new BigDecimal("350.00"),
                        new BigDecimal("250.00"), new BigDecimal("200.00"));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void publishEvent_throwsIllegalStateWhenEventIsNotDraft() {
        when(eventRepository.findById(EVENT_ID))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, hall(List.of()))));

        assertThatThrownBy(() -> service.publishEvent(EVENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only draft event can be published");

        verifyNoInteractions(eventSeatRepository);
    }

    @Test
    void publishEvent_throwsSeatsAlreadyGeneratedWhenEventSeatsExist() {
        when(eventRepository.findById(EVENT_ID))
                .thenReturn(Optional.of(event(EventStatus.DRAFT, hall(List.of()))));
        when(eventSeatRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.publishEvent(EVENT_ID))
                .isInstanceOf(SeatsAlreadyGeneratedException.class)
                .hasMessageContaining(EVENT_ID.toString());

        verify(eventSeatRepository, never()).saveAll(anyList());
    }

    @Test
    void publishEvent_throwsEventNotFoundWhenEventIsMissing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publishEvent(EVENT_ID))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void cancelEvent_marksPublishedEventCancelled() {
        Event published = event(EventStatus.PUBLISHED, hall(List.of()));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(published));

        service.cancelEvent(EVENT_ID);

        assertThat(published.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void cancelEvent_throwsIllegalStateWhenEventIsNotPublished() {
        when(eventRepository.findById(EVENT_ID))
                .thenReturn(Optional.of(event(EventStatus.DRAFT, hall(List.of()))));

        assertThatThrownBy(() -> service.cancelEvent(EVENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only published event can be cancelled");
    }

    @Test
    void cancelEvent_throwsEventNotFoundWhenEventIsMissing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelEvent(EVENT_ID))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void findByOrganizerId_mapsEveryEvent() {
        when(eventRepository.findByOrganizerId(ORGANIZER_ID)).thenReturn(List.of(
                event(EventStatus.DRAFT, hall(List.of())),
                event(EventStatus.PUBLISHED, hall(List.of()))));

        List<EventResponse> responses = service.findByOrganizerId(ORGANIZER_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(EventResponse::status)
                .containsExactly(EventStatus.DRAFT, EventStatus.PUBLISHED);
    }

    @Test
    void findPublishedEventByCityAndDate_queriesPublishedEventsWithSorting() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        when(eventRepository.findPublishedEvents(eq(EventStatus.PUBLISHED), eq("Kyiv"), eq(date), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event(EventStatus.PUBLISHED, hall(List.of()))),
                        PageRequest.of(0, 10), 1));

        Page<EventResponse> page = service.findPublishedEventByCityAndDate("Kyiv", date, 0, 10, "startsAt");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findPublishedEvents(eq(EventStatus.PUBLISHED), eq("Kyiv"), eq(date), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "startsAt"));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findPublishedEventsByVenue_queriesPublishedEventsOfVenue() {
        when(eventRepository.findPublishedEventsByVenueId(eq(EventStatus.PUBLISHED), eq(VENUE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event(EventStatus.PUBLISHED, hall(List.of()))),
                        PageRequest.of(0, 10), 1));

        Page<EventResponse> page = service.findPublishedEventsByVenue(VENUE_ID, 0, 10, "id");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).hallId()).isEqualTo(HALL_ID);
    }
}
