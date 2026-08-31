package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.responses.SeatResponse;
import com.sorokaandriy.event_service.entity.Seat;
import org.springframework.stereotype.Service;

@Service
public class SeatMapper {

    public SeatResponse fromSeatToSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .rowNumber(seat.getRowNumber())
                .seatNumber(seat.getSeatNumber())
                .sector(seat.getSector())
                .hallId(seat.getHall().getId())
                .build();
    }


}
