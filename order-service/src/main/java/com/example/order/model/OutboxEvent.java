package com.example.order.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable=false) private String aggregateType;
  @Column(nullable=false) private String aggregateId;
  @Column(nullable=false) private String type;
  @Column(nullable=false, columnDefinition="TEXT") private String payload; // store as text; DB has JSONB column
  @Column(nullable=false) private Instant createdAt;
  private Instant publishedAt;
}
