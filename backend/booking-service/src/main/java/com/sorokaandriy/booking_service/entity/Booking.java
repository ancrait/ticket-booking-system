package com.sorokaandriy.booking_service.entity;

import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String email;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(name = "event_title", nullable = false)
    private String eventTitle;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at")
    private Instant updatedAt;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingItem> bookingItems;
}
