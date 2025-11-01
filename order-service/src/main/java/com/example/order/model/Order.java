package com.example.order.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="orders")
public class Order {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long customerId;
  @ElementCollection
  @CollectionTable(name="order_items", joinColumns=@JoinColumn(name="order_id"))
  private List<OrderItem> items = new ArrayList<>();
  private String status; // CREATED, FAILED
}

