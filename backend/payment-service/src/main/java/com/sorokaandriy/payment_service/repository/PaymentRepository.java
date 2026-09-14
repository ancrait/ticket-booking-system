package com.sorokaandriy.payment_service.repository;

import com.sorokaandriy.payment_service.entity.Payment;
import com.sorokaandriy.payment_service.entity.enumeration.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsPaymentByBookingId(UUID bookingId);

    boolean existsPaymentByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    Optional<Payment> findByProviderPaymentId(String id);
}
