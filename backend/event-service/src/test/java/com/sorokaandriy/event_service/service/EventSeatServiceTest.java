package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.ConfirmSeatsRequest;
import com.sorokaandriy.event_service.dto.requests.HeldSeatRequest;
import com.sorokaandriy.event_service.dto.requests.ReleaseSeatsRequest;
import com.sorokaandriy.event_service.dto.responses.EventSeatResponse;
import com.sorokaandriy.event_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.event_service.dto.responses.HeldSeatsResponse;
import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.EventSeat;
import com.sorokaandriy.event_service.entity.Seat;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import com.sorokaandriy.event_service.exception.EventNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsNotAvailableException;
import com.sorokaandriy.event_service.repository.EventRepository;
import com.sorokaandriy.event_service.repository.EventSeatRepository;
import com.sorokaandriy.event_service.service.mapper.EventSeatMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
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
class EventSeatServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIRST_SEAT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SECOND_SEAT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    private EventSeatService service;

    @BeforeEach
    void setUp() {
        service = new EventSeatService(eventRepository, eventSeatRepository, new EventSeatMapper());
    }

    private Event event() {
        return Event.builder()
                .id(EVENT_ID)
                .title("Concert")
                .status(EventStatus.PUBLISHED)
                .build();
    }

    private EventSeat eventSeat(UUID id, EventSeatStatus status) {
        return EventSeat.builder()
                .id(id)
                .price(new BigDecimal("250.00"))
                .status(status)
                .event(event())
                .seat(Seat.builder()
                        .id(id)
                        .rowNumber(1)
                        .seatNumber(2)
                        .sector("B")
                        .build())
                .build();
    }

    @Test
    void findAllEventSeats_sortsDescendingByRequestedField() {
        when(eventSeatRepository.findByEventId(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.FREE)), PageRequest.of(0, 10), 1));

        Page<EventSeatResponse> page = service.findAllEventSeats(0, 10, "price", EVENT_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventSeatRepository).findByEventId(any(UUID.class), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "price"));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).price()).isEqualByComparingTo("250.00");
    }

    @Test
    void findAvailableEventSeats_queriesOnlyFreeSeats() {
        when(eventSeatRepository.findEventSeatByEventIdAndStatus(eq(EVENT_ID), eq(EventSeatStatus.FREE),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.FREE)), PageRequest.of(0, 10), 1));

        Page<EventSeatResponse> page = service.findAvailableEventSeats(0, 10, "id", EVENT_ID);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventSeatRepository).findEventSeatByEventIdAndStatus(eq(EVENT_ID), eq(EventSeatStatus.FREE),
                captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).status()).isEqualTo(EventSeatStatus.FREE);
    }

    @Test
    void holdSeats_marksSeatsHeldAndReturnsEventTitleWithSeatInfo() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID, SECOND_SEAT_ID);
        EventSeat first = eventSeat(FIRST_SEAT_ID, EventSeatStatus.FREE);
        EventSeat second = eventSeat(SECOND_SEAT_ID, EventSeatStatus.FREE);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event()));
        when(eventSeatRepository.findAllByEventIdAndIdInAndStatus(EVENT_ID, seatIds, EventSeatStatus.FREE))
                .thenReturn(List.of(first, second));

        HeldSeatsResponse response = service.holdSeats(EVENT_ID, new HeldSeatRequest(seatIds));

        assertThat(first.getStatus()).isEqualTo(EventSeatStatus.HELD);
        assertThat(second.getStatus()).isEqualTo(EventSeatStatus.HELD);
        assertThat(response.eventTitle()).isEqualTo("Concert");
        assertThat(response.seats()).hasSize(2);
        assertThat(response.seats()).extracting(HeldSeatInfo::eventSeatId)
                .containsExactly(FIRST_SEAT_ID, SECOND_SEAT_ID);
        assertThat(response.seats().get(0).sector()).isEqualTo("B");
    }

    @Test
    void holdSeats_throwsEventNotFoundWhenEventIsMissing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.holdSeats(EVENT_ID, new HeldSeatRequest(List.of(FIRST_SEAT_ID))))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(EVENT_ID.toString());

        verifyNoInteractions(eventSeatRepository);
    }

    @Test
    void holdSeats_throwsSeatsNotAvailableWhenSomeSeatsAreNotFree() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID, SECOND_SEAT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event()));
        when(eventSeatRepository.findAllByEventIdAndIdInAndStatus(EVENT_ID, seatIds, EventSeatStatus.FREE))
                .thenReturn(List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.FREE)));

        assertThatThrownBy(() -> service.holdSeats(EVENT_ID, new HeldSeatRequest(seatIds)))
                .isInstanceOf(SeatsNotAvailableException.class)
                .hasMessage("Some seats are already taken");
    }

    @Test
    void releaseSeats_freesHeldSeats() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID, SECOND_SEAT_ID);
        EventSeat held = eventSeat(FIRST_SEAT_ID, EventSeatStatus.HELD);
        EventSeat free = eventSeat(SECOND_SEAT_ID, EventSeatStatus.FREE);
        when(eventSeatRepository.findAllByEventIdAndIdIn(EVENT_ID, seatIds)).thenReturn(List.of(held, free));

        service.releaseSeats(EVENT_ID, new ReleaseSeatsRequest(seatIds));

        assertThat(held.getStatus()).isEqualTo(EventSeatStatus.FREE);
        assertThat(free.getStatus()).isEqualTo(EventSeatStatus.FREE);
    }

    @Test
    void releaseSeats_throwsSeatsNotAvailableWhenSeatIsSold() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID);
        when(eventSeatRepository.findAllByEventIdAndIdIn(EVENT_ID, seatIds))
                .thenReturn(List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.SOLD)));

        assertThatThrownBy(() -> service.releaseSeats(EVENT_ID, new ReleaseSeatsRequest(seatIds)))
                .isInstanceOf(SeatsNotAvailableException.class)
                .hasMessage("Some seats are already sold");
    }

    @Test
    void releaseSeats_throwsSeatsNotAvailableWhenSomeSeatsAreMissing() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID, SECOND_SEAT_ID);
        when(eventSeatRepository.findAllByEventIdAndIdIn(EVENT_ID, seatIds))
                .thenReturn(List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.HELD)));

        assertThatThrownBy(() -> service.releaseSeats(EVENT_ID, new ReleaseSeatsRequest(seatIds)))
                .isInstanceOf(SeatsNotAvailableException.class)
                .hasMessage("Some seats not found");
    }

    @Test
    void confirmSeats_sellsHeldSeats() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID, SECOND_SEAT_ID);
        EventSeat first = eventSeat(FIRST_SEAT_ID, EventSeatStatus.HELD);
        EventSeat second = eventSeat(SECOND_SEAT_ID, EventSeatStatus.HELD);
        when(eventSeatRepository.findAllByEventIdAndIdInAndStatus(EVENT_ID, seatIds, EventSeatStatus.HELD))
                .thenReturn(List.of(first, second));

        service.confirmSeats(EVENT_ID, new ConfirmSeatsRequest(seatIds));

        assertThat(first.getStatus()).isEqualTo(EventSeatStatus.SOLD);
        assertThat(second.getStatus()).isEqualTo(EventSeatStatus.SOLD);
    }

    @Test
    void confirmSeats_throwsSeatsNotAvailableWhenSeatsAreNotHeld() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID, SECOND_SEAT_ID);
        when(eventSeatRepository.findAllByEventIdAndIdInAndStatus(EVENT_ID, seatIds, EventSeatStatus.HELD))
                .thenReturn(List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.HELD)));

        assertThatThrownBy(() -> service.confirmSeats(EVENT_ID, new ConfirmSeatsRequest(seatIds)))
                .isInstanceOf(SeatsNotAvailableException.class)
                .hasMessage("Some seats are not in HELD status");
    }

    @Test
    void confirmSeats_doesNotTouchEventRepository() {
        List<UUID> seatIds = List.of(FIRST_SEAT_ID);
        when(eventSeatRepository.findAllByEventIdAndIdInAndStatus(EVENT_ID, seatIds, EventSeatStatus.HELD))
                .thenReturn(List.of(eventSeat(FIRST_SEAT_ID, EventSeatStatus.HELD)));

        service.confirmSeats(EVENT_ID, new ConfirmSeatsRequest(seatIds));

        verifyNoInteractions(eventRepository);
        verify(eventSeatRepository, never()).saveAll(anyList());
    }
}
