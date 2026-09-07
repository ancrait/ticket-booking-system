package com.sorokaandriy.payment_service.dto.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record BookingCreatedEvent(
        UUID bookingId,
        UUID userId,
        String email,
        UUID eventId,
        String eventTitle,
        BigDecimal totalPrice,
        List<UUID> seatIds
) {


}
