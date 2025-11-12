package com.example.store.model;
import jakarta.persistence.*;
import lombok.*;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable=false) private String name;
  @Column(nullable=false) private Double price;
  @Column(nullable=false) private Integer quantity = 0; // default;
}
