package com.sorokaandriy.booking_service.repository;

import com.sorokaandriy.booking_service.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
}
