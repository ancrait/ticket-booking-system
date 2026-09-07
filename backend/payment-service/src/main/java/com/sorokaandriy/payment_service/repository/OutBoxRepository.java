package com.sorokaandriy.payment_service.repository;

import com.sorokaandriy.payment_service.entity.OutBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutBoxRepository extends JpaRepository<OutBox, UUID> {

    List<OutBox> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
