package com.sorokaandriy.booking_service.dto.responses;

import lombok.Builder;

import java.util.List;

@Builder
public record HeldSeatsResponse(
        String eventTitle,
        List<HeldSeatInfo> seats
) {
}
