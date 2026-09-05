package com.sorokaandriy.booking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "outbox")
public class OutBox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private String payload;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    @Column(name = "published_at")
    private Instant publishedAt;

}
