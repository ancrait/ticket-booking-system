package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.requests.VenueRequest;
import com.sorokaandriy.event_service.dto.responses.VenueResponse;
import com.sorokaandriy.event_service.entity.Venue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueMapperTest {

    private static final UUID VENUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final VenueMapper mapper = new VenueMapper();

    @Test
    void fromVenueRequestToVenue_mapsFieldsAndStampCreatedAt() {
        Venue venue = mapper.fromVenueRequestToVenue(new VenueRequest("Arena", "Kyiv", "Main St 1"));

        assertThat(venue.getName()).isEqualTo("Arena");
        assertThat(venue.getCity()).isEqualTo("Kyiv");
        assertThat(venue.getAddress()).isEqualTo("Main St 1");
        assertThat(venue.getId()).isNull();
        assertThat(venue.getCreatedAt()).isNotNull();
        assertThat(venue.getUpdatedAt()).isNull();
    }

    @Test
    void fromVenueToVenueResponse_mapsAllFields() {
        Venue venue = Venue.builder()
                .id(VENUE_ID)
                .name("Arena")
                .city("Kyiv")
                .address("Main St 1")
                .createdAt(CREATED_AT)
                .build();

        VenueResponse response = mapper.fromVenueToVenueResponse(venue);

        assertThat(response.id()).isEqualTo(VENUE_ID);
        assertThat(response.name()).isEqualTo("Arena");
        assertThat(response.city()).isEqualTo("Kyiv");
        assertThat(response.address()).isEqualTo("Main St 1");
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }
}
