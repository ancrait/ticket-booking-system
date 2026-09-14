package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.responses.EventSeatResponse;
import com.sorokaandriy.event_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.event_service.entity.EventSeat;
import org.springframework.stereotype.Service;

@Service
public class EventSeatMapper {

    public EventSeatResponse fromEventSeatToEventSeatResponse(EventSeat eventSeat){

        return EventSeatResponse.builder()
                .id(eventSeat.getId())
                .seatId(eventSeat.getSeat().getId())
                .rowNumber(eventSeat.getSeat().getRowNumber())
                .seatNumber(eventSeat.getSeat().getSeatNumber())
                .sector(eventSeat.getSeat().getSector())
                .price(eventSeat.getPrice())
                .status(eventSeat.getStatus())
                .build();
    }

    public HeldSeatInfo fromEventSeatToHeldSeatInfo(EventSeat eventSeat){

        return HeldSeatInfo.builder()
                .eventSeatId(eventSeat.getId())
                .rowNumber(eventSeat.getSeat().getRowNumber())
                .seatNumber(eventSeat.getSeat().getSeatNumber())
                .sector(eventSeat.getSeat().getSector())
                .price(eventSeat.getPrice())
                .build();
    }
}
