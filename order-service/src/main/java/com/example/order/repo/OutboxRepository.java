package com.example.order.repo;
import com.example.order.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
  @Query("select e from OutboxEvent e where e.publishedAt is null order by e.id asc")
  List<OutboxEvent> findUnpublished();
}
