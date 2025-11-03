package com.example.order.repo;

import com.example.order.model.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findAllByOrderByIdAsc();

    // (опционально, если нужно постранично:)
    Page<OutboxEvent> findAll(Pageable pageable);
}
