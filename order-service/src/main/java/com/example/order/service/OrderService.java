package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OutboxEvent;
import com.example.order.repo.OrderRepository;
import com.example.order.repo.OutboxRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Service
public class OrderService {
    private final OrderRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final WebClient storeClient;
    private final OutboxRepository outbox;

    public OrderService(OrderRepository repo,
                        KafkaTemplate<String, String> kafka,
                        OutboxRepository outbox,
                        @Value("${app.storeUrl}") String storeUrl) {
        this.repo = repo;
        this.kafka = kafka;
        this.outbox = outbox;
        this.storeClient = WebClient.builder()
                .baseUrl(storeUrl) // напр. http://store-service:8081
                .build();
    }

    // DTO ответа store-service: /api/products/{id}
    public record ProductDto(Long id, String name, BigDecimal price, Integer quantity) {}

    @Transactional
    public Order create(Order order) {
        // 1) Проставляем цену для каждой позиции (историческая фиксация)
        for (OrderItem item : order.getItems()) {
            ProductDto product = storeClient.get()
                    .uri("/api/products/{id}", item.getProductId())
                    .retrieve()
                    .bodyToMono(ProductDto.class)
                    .block();

            if (product == null || product.price() == null) {
                throw new EntityNotFoundException(
                        "Product %d not found or has no price".formatted(item.getProductId()));
            }

            // фиксируем цену на момент оформления
            item.setPrice(product.price().doubleValue());
            // ВАЖНО: НЕТ item.setOrder(order) — у тебя Embeddable + ElementCollection
        }

        // 2) Статус
        order.setStatus("CREATED");

        // 3) Сохраняем заказ (Hibernate сам вставит строки в order_items через @ElementCollection)
        Order saved = repo.save(order);

        // 4) Outbox событие
        var mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder().build();
        String payload = mapper.createObjectNode()
                .put("orderId", saved.getId())
                .put("customerId", saved.getCustomerId())
                .set("items", mapper.valueToTree(saved.getItems()))
                .toString();

        outbox.save(OutboxEvent.builder()
                .aggregateType("Order")
                .aggregateId(saved.getId().toString())
                .eventType("OrderCreated")
                .payload(payload)
                .createdAt(java.time.Instant.now())
                .build());

        return saved;
    }
}
