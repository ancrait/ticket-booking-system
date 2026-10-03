package com.sorokaandriy.event_service.service;

import com.sorokaandriy.event_service.dto.requests.VenueRequest;
import com.sorokaandriy.event_service.dto.responses.VenueResponse;
import com.sorokaandriy.event_service.entity.Venue;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import com.sorokaandriy.event_service.repository.VenueRepository;
import com.sorokaandriy.event_service.service.mapper.VenueMapper;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    private static final UUID VENUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private VenueRepository venueRepository;

    private VenueService service;

    @BeforeEach
    void setUp() {
        service = new VenueService(venueRepository, new VenueMapper());
    }

    private Venue venue(String city) {
        return Venue.builder()
                .id(VENUE_ID)
                .name("Arena")
                .city(city)
                .address("Main St 1")
                .createdAt(CREATED_AT)
                .build();
    }

    @Test
    void createVenue_savesVenueAndReturnsResponse() {
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue saved = invocation.getArgument(0);
            saved.setId(VENUE_ID);
            return saved;
        });

        VenueResponse response = service.createVenue(new VenueRequest("Arena", "Kyiv", "Main St 1"));

        assertThat(response.id()).isEqualTo(VENUE_ID);
        assertThat(response.name()).isEqualTo("Arena");
        assertThat(response.city()).isEqualTo("Kyiv");
        assertThat(response.address()).isEqualTo("Main St 1");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void findVenue_returnsMappedVenue() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue("Kyiv")));

        VenueResponse response = service.findVenue(VENUE_ID);

        assertThat(response.id()).isEqualTo(VENUE_ID);
        assertThat(response.city()).isEqualTo("Kyiv");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void findVenue_throwsVenueNotFoundWhenVenueIsMissing() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findVenue(VENUE_ID))
                .isInstanceOf(VenueNotFoundException.class)
                .hasMessageContaining(VENUE_ID.toString());
    }

    @Test
    void findAllVenues_filtersByCityWhenCityIsProvided() {
        when(venueRepository.findByCityIgnoreCase(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(venue("Kyiv")), PageRequest.of(0, 10), 1));

        Page<VenueResponse> page = service.findAllVenues(0, 10, "name", "Kyiv");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(venueRepository).findByCityIgnoreCase(any(String.class), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "name"));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).city()).isEqualTo("Kyiv");
        verify(venueRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAllVenues_ignoresBlankCityAndReturnsAll() {
        when(venueRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(venue("Kyiv"), venue("Lviv")), PageRequest.of(0, 10), 2));

        Page<VenueResponse> page = service.findAllVenues(0, 10, "id", "   ");

        assertThat(page.getContent()).hasSize(2);
        verify(venueRepository, never()).findByCityIgnoreCase(any(String.class), any(Pageable.class));
    }

    @Test
    void findAllVenues_returnsAllWhenCityIsNull() {
        when(venueRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(venue("Kyiv")), PageRequest.of(0, 10), 1));

        Page<VenueResponse> page = service.findAllVenues(0, 10, "id", null);

        assertThat(page.getContent()).hasSize(1);
        verify(venueRepository, never()).findByCityIgnoreCase(any(String.class), any(Pageable.class));
    }

    @Test
    void updateVenue_overwritesFieldsAndTouchesUpdatedAt() {
        Venue venue = venue("Kyiv");
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue));
        when(venueRepository.save(venue)).thenReturn(venue);

        VenueResponse response = service.updateVenue(VENUE_ID, new VenueRequest("Palace", "Lviv", "Second St 2"));

        assertThat(response.name()).isEqualTo("Palace");
        assertThat(response.city()).isEqualTo("Lviv");
        assertThat(response.address()).isEqualTo("Second St 2");
        assertThat(venue.getUpdatedAt()).isNotNull();
        assertThat(venue.getCreatedAt()).isEqualTo(CREATED_AT);
        verify(venueRepository).save(venue);
    }

    @Test
    void updateVenue_throwsVenueNotFoundWhenVenueIsMissing() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateVenue(VENUE_ID, new VenueRequest("Palace", "Lviv", "Second St 2")))
                .isInstanceOf(VenueNotFoundException.class);

        verify(venueRepository, never()).save(any());
    }

    @Test
    void deleteVenue_deletesExistingVenue() {
        Venue venue = venue("Kyiv");
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue));

        service.deleteVenue(VENUE_ID);

        verify(venueRepository).delete(venue);
    }

    @Test
    void deleteVenue_throwsVenueNotFoundAndDoesNotDelete() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteVenue(VENUE_ID))
                .isInstanceOf(VenueNotFoundException.class);

        verify(venueRepository, never()).delete(any());
    }
}
