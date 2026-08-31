package com.sorokaandriy.event_service.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;
    @Column(nullable = false)
    private String sector;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;
    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL)
    private List<EventSeat> eventSeats;
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Builder.Default
    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();



}
