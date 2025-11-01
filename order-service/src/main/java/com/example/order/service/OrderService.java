package com.example.order.service;
import com.example.order.model.Order;
import com.example.order.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import com.example.order.model.OutboxEvent;
import com.example.order.repo.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OrderService {
  private final OrderRepository repo;
  private final KafkaTemplate<String, String> kafka;
  private final WebClient storeClient;
  private final OutboxRepository outbox;

  public OrderService(OrderRepository repo, KafkaTemplate<String, String> kafka, OutboxRepository outbox, @Value("${app.storeUrl}") String storeUrl){
    this.repo = repo;
    this.kafka = kafka;
    this.outbox = outbox;
    this.storeClient = WebClient.create(storeUrl);
  }

  @Transactional
  public Order create(Order order){
    // saga: no sync reserve; reserved asynchronously by store-service
    order.setStatus("CREATED");
    Order saved = repo.save(order);
    // serialize minimal payload (could be full order)
    String payload = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().createObjectNode().put("orderId", saved.getId()).put("customerId", saved.getCustomerId()).set("items", com.fasterxml.jackson.databind.json.JsonMapper.builder().build().valueToTree(saved.getItems())).toString();
    outbox.save(OutboxEvent.builder()
        .aggregateType("Order")
        .aggregateId(saved.getId().toString())
        .type("OrderCreated")
        .payload(payload)
        .createdAt(java.time.Instant.now())
        .build());
    return saved;
  }
}
