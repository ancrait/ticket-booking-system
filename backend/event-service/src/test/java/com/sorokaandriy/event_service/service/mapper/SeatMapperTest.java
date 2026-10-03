package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.responses.SeatResponse;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Seat;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SeatMapperTest {

    private static final UUID SEAT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HALL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final SeatMapper mapper = new SeatMapper();

    @Test
    void fromSeatToSeatResponse_mapsAllFieldsAndFlattensHallId() {
        Seat seat = Seat.builder()
                .id(SEAT_ID)
                .rowNumber(3)
                .seatNumber(12)
                .sector("VIP")
                .hall(Hall.builder().id(HALL_ID).build())
                .build();

        SeatResponse response = mapper.fromSeatToSeatResponse(seat);

        assertThat(response.id()).isEqualTo(SEAT_ID);
        assertThat(response.rowNumber()).isEqualTo(3);
        assertThat(response.seatNumber()).isEqualTo(12);
        assertThat(response.sector()).isEqualTo("VIP");
        assertThat(response.hallId()).isEqualTo(HALL_ID);
    }
}
