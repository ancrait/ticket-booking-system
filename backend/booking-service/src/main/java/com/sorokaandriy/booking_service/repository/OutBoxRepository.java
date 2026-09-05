package com.sorokaandriy.booking_service.repository;

import com.sorokaandriy.booking_service.entity.OutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutBoxRepository extends JpaRepository<OutBox, UUID> {
    List<OutBox> findByPublishedAtIsNull();

    List<OutBox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
