package com.sorokaandriy.booking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "booking_items")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_seat_id", nullable = false)
    private UUID eventSeatId;
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;
    @Column(nullable = false)
    private BigDecimal price;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
}
