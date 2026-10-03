package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Venue;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HallMapperTest {

    private static final UUID HALL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final HallMapper mapper = new HallMapper();

    @Test
    void fromHallRequestToHall_mapsSizesAndLeavesVenueUnset() {
        Hall hall = mapper.fromHallRequestToHall(new HallRequest("Hall 1", 5, 8));

        assertThat(hall.getName()).isEqualTo("Hall 1");
        assertThat(hall.getRowsCount()).isEqualTo(5);
        assertThat(hall.getSeatsPerRow()).isEqualTo(8);
        assertThat(hall.getVenue()).isNull();
        assertThat(hall.getId()).isNull();
        assertThat(hall.getCreatedAt()).isNotNull();
    }

    @Test
    void fromHallToHallResponse_mapsAllFieldsAndFlattensVenueId() {
        Hall hall = Hall.builder()
                .id(HALL_ID)
                .name("Hall 1")
                .rowsCount(5)
                .seatsPerRow(8)
                .venue(Venue.builder().id(VENUE_ID).build())
                .build();

        HallResponse response = mapper.fromHallToHallResponse(hall);

        assertThat(response.id()).isEqualTo(HALL_ID);
        assertThat(response.name()).isEqualTo("Hall 1");
        assertThat(response.rowsCount()).isEqualTo(5);
        assertThat(response.seatsPerRow()).isEqualTo(8);
        assertThat(response.venueId()).isEqualTo(VENUE_ID);
    }
}
