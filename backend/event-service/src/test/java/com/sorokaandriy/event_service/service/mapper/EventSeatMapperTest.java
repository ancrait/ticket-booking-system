package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.responses.EventSeatResponse;
import com.sorokaandriy.event_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.event_service.entity.EventSeat;
import com.sorokaandriy.event_service.entity.Seat;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventSeatMapperTest {

    private static final UUID EVENT_SEAT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEAT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final EventSeatMapper mapper = new EventSeatMapper();

    private EventSeat eventSeat() {
        return EventSeat.builder()
                .id(EVENT_SEAT_ID)
                .price(new BigDecimal("350.00"))
                .status(EventSeatStatus.FREE)
                .seat(Seat.builder()
                        .id(SEAT_ID)
                        .rowNumber(2)
                        .seatNumber(7)
                        .sector("A")
                        .build())
                .build();
    }

    @Test
    void fromEventSeatToEventSeatResponse_mapsSeatFieldsAndPrice() {
        EventSeatResponse response = mapper.fromEventSeatToEventSeatResponse(eventSeat());

        assertThat(response.id()).isEqualTo(EVENT_SEAT_ID);
        assertThat(response.seatId()).isEqualTo(SEAT_ID);
        assertThat(response.rowNumber()).isEqualTo(2);
        assertThat(response.seatNumber()).isEqualTo(7);
        assertThat(response.sector()).isEqualTo("A");
        assertThat(response.price()).isEqualByComparingTo("350.00");
        assertThat(response.status()).isEqualTo(EventSeatStatus.FREE);
    }

    @Test
    void fromEventSeatToHeldSeatInfo_exposesEventSeatIdWithoutStatus() {
        HeldSeatInfo info = mapper.fromEventSeatToHeldSeatInfo(eventSeat());

        assertThat(info.eventSeatId()).isEqualTo(EVENT_SEAT_ID);
        assertThat(info.rowNumber()).isEqualTo(2);
        assertThat(info.seatNumber()).isEqualTo(7);
        assertThat(info.sector()).isEqualTo("A");
        assertThat(info.price()).isEqualByComparingTo("350.00");
    }
}
