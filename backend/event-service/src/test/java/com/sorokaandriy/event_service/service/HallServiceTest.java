package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.dto.responses.SeatResponse;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Seat;
import com.sorokaandriy.event_service.entity.Venue;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import com.sorokaandriy.event_service.repository.HallRepository;
import com.sorokaandriy.event_service.repository.SeatRepository;
import com.sorokaandriy.event_service.repository.VenueRepository;
import com.sorokaandriy.event_service.service.mapper.HallMapper;
import com.sorokaandriy.event_service.service.mapper.SeatMapper;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HallServiceTest {

    private static final UUID HALL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private SeatRepository seatRepository;

    @Captor
    private ArgumentCaptor<List<Seat>> seatsCaptor;

    private HallService service;

    @BeforeEach
    void setUp() {
        service = new HallService(venueRepository, hallRepository, new HallMapper(), seatRepository, new SeatMapper());
    }

    private Venue venue() {
        return Venue.builder()
                .id(VENUE_ID)
                .name("Arena")
                .city("Kyiv")
                .address("Main St 1")
                .build();
    }

    private Hall hall(int rowsCount, int seatsPerRow) {
        return Hall.builder()
                .id(HALL_ID)
                .name("Hall 1")
                .rowsCount(rowsCount)
                .seatsPerRow(seatsPerRow)
                .venue(venue())
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    void createHall_attachesVenueAndSavesHall() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue()));
        when(hallRepository.save(any(Hall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HallResponse response = service.createHall(VENUE_ID, new HallRequest("Hall 1", 5, 8));

        assertThat(response.name()).isEqualTo("Hall 1");
        assertThat(response.rowsCount()).isEqualTo(5);
        assertThat(response.seatsPerRow()).isEqualTo(8);
        assertThat(response.venueId()).isEqualTo(VENUE_ID);

        ArgumentCaptor<Hall> captor = ArgumentCaptor.forClass(Hall.class);
        verify(hallRepository).save(captor.capture());
        assertThat(captor.getValue().getVenue().getId()).isEqualTo(VENUE_ID);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void createHall_throwsVenueNotFoundAndDoesNotSave() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createHall(VENUE_ID, new HallRequest("Hall 1", 5, 8)))
                .isInstanceOf(VenueNotFoundException.class)
                .hasMessageContaining(VENUE_ID.toString());

        verifyNoInteractions(hallRepository);
    }

    @Test
    void findHallsByVenueId_mapsEveryHallOfVenue() {
        Venue venue = venue();
        venue.setHalls(List.of(hall(5, 8), hall(3, 4)));
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue));

        List<HallResponse> responses = service.findHallsByVenueId(VENUE_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(response -> assertThat(response.venueId()).isEqualTo(VENUE_ID));
        assertThat(responses).extracting(HallResponse::rowsCount).containsExactly(5, 3);
    }

    @Test
    void findHallsByVenueId_throwsVenueNotFoundWhenVenueIsMissing() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findHallsByVenueId(VENUE_ID))
                .isInstanceOf(VenueNotFoundException.class);
    }

    @Test
    void findHallById_returnsMappedHall() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall(5, 8)));

        HallResponse response = service.findHallById(HALL_ID);

        assertThat(response.id()).isEqualTo(HALL_ID);
        assertThat(response.name()).isEqualTo("Hall 1");
        assertThat(response.venueId()).isEqualTo(VENUE_ID);
    }

    @Test
    void findHallById_throwsHallNotFoundWhenHallIsMissing() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findHallById(HALL_ID))
                .isInstanceOf(HallNotFoundException.class)
                .hasMessageContaining(HALL_ID.toString());
    }

    @Test
    void updateHall_overwritesFieldsAndTouchesUpdatedAt() {
        Hall hall = hall(5, 8);
        hall.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));
        when(hallRepository.save(hall)).thenReturn(hall);

        HallResponse response = service.updateHall(HALL_ID, new HallRequest("Renamed", 7, 9));

        assertThat(response.name()).isEqualTo("Renamed");
        assertThat(response.rowsCount()).isEqualTo(7);
        assertThat(response.seatsPerRow()).isEqualTo(9);
        assertThat(hall.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T10:00:00Z"));
        verify(hallRepository).save(hall);
    }

    @Test
    void updateHall_throwsHallNotFoundWhenHallIsMissing() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateHall(HALL_ID, new HallRequest("Renamed", 7, 9)))
                .isInstanceOf(HallNotFoundException.class);

        verify(hallRepository, never()).save(any());
    }

    @Test
    void deleteHall_deletesExistingHall() {
        Hall hall = hall(5, 8);
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));

        service.deleteHall(HALL_ID);

        verify(hallRepository).delete(hall);
    }

    @Test
    void deleteHall_throwsHallNotFoundAndDoesNotDelete() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteHall(HALL_ID))
                .isInstanceOf(HallNotFoundException.class);

        verify(hallRepository, never()).delete(any());
    }

    @Test
    void generateSeats_createsRowsTimesSeatsWithSectorPerRowBand() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall(3, 2)));
        when(seatRepository.existsByHallId(HALL_ID)).thenReturn(false);
        when(seatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SeatResponse> seats = service.generateSeats(HALL_ID);

        assertThat(seats).hasSize(6);
        assertThat(seats).extracting(SeatResponse::rowNumber).containsExactly(1, 1, 2, 2, 3, 3);
        assertThat(seats).extracting(SeatResponse::seatNumber).containsExactly(1, 2, 1, 2, 1, 2);
        assertThat(seats).extracting(SeatResponse::sector)
                .containsExactly("VIP", "VIP", "A", "A", "B", "B");
        assertThat(seats).allSatisfy(seat -> assertThat(seat.hallId()).isEqualTo(HALL_ID));

        verify(seatRepository).saveAll(seatsCaptor.capture());
        assertThat(seatsCaptor.getValue()).allSatisfy(seat -> assertThat(seat.getHall().getId()).isEqualTo(HALL_ID));
    }

    @Test
    void generateSeats_throwsHallNotFoundWhenHallIsMissing() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateSeats(HALL_ID))
                .isInstanceOf(HallNotFoundException.class);

        verifyNoInteractions(seatRepository);
    }

    @Test
    void generateSeats_throwsSeatsAlreadyGeneratedWhenHallHasSeats() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall(3, 2)));
        when(seatRepository.existsByHallId(HALL_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.generateSeats(HALL_ID))
                .isInstanceOf(SeatsAlreadyGeneratedException.class)
                .hasMessage("Seats already exist");

        verify(seatRepository, never()).saveAll(anyList());
    }

    @Test
    void findSeatsByHallId_sortsDescendingByRequestedField() {
        Hall hall = hall(3, 2);
        Seat seat = Seat.builder()
                .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .rowNumber(1)
                .seatNumber(1)
                .sector("VIP")
                .hall(hall)
                .build();
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.of(hall));
        when(seatRepository.findAllByHall(any(Hall.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(seat), PageRequest.of(0, 10), 1));

        Page<SeatResponse> page = service.findSeatsByHallId(HALL_ID, 0, 10, "rowNumber");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(seatRepository).findAllByHall(any(Hall.class), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "rowNumber"));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).sector()).isEqualTo("VIP");
    }

    @Test
    void findSeatsByHallId_throwsHallNotFoundWhenHallIsMissing() {
        when(hallRepository.findById(HALL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findSeatsByHallId(HALL_ID, 0, 10, "id"))
                .isInstanceOf(HallNotFoundException.class);

        verifyNoInteractions(seatRepository);
    }
}
