package com.example.store.kafka;

import com.example.store.model.Product;
import com.example.store.repo.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderCreatedConsumer {

  private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

  private final ProductRepository productRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${app.orderConfirmedTopic:order.confirmed}")
  private String orderConfirmedTopic;

  @Value("${app.orderFailedTopic:order.failed}")
  private String orderFailedTopic;

  public OrderCreatedConsumer(ProductRepository productRepository,
                              KafkaTemplate<String, String> kafkaTemplate) {
    this.productRepository = productRepository;
    this.kafkaTemplate = kafkaTemplate;
  }

  @Transactional
  @KafkaListener(topics = "${app.orderTopic:order.created}", groupId = "store-service")
  public void onMessage(ConsumerRecord<String, String> record) {
    log.info("Consumed order event key={} value={}", record.key(), record.value());

    try {
      JsonNode root = objectMapper.readTree(record.value());
      long orderId = root.path("orderId").asLong();
      ArrayNode items = (ArrayNode) root.path("items");

      // 1) Проверяем наличие товара
      for (JsonNode item : items) {
        long productId = item.path("productId").asLong();
        int qty = item.path("quantity").asInt(0);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product %d not found for order %d".formatted(productId, orderId)));

        if (product.getQuantity() == null || product.getQuantity() < qty) {
          log.warn("Not enough stock for product {}: have {}, need {}. Order {} FAILED",
                  productId, product.getQuantity(), qty, orderId);

          // уведомляем order-service, что заказ не прошёл
          kafkaTemplate.send(orderFailedTopic, String.valueOf(orderId), record.value());
          return;
        }
      }

      // 2) Хватает – списываем со склада
      for (JsonNode item : items) {
        long productId = item.path("productId").asLong();
        int qty = item.path("quantity").asInt(0);

        Product product = productRepository.findById(productId).orElseThrow();
        product.setQuantity(product.getQuantity() - qty);
        productRepository.save(product);

        log.info("Product {} stock decreased by {}. New quantity={}",
                productId, qty, product.getQuantity());
      }

      // 3) Всё ОК – подтверждаем заказ
      kafkaTemplate.send(orderConfirmedTopic, String.valueOf(orderId), record.value());
      log.info("Order {} CONFIRMED", orderId);

    } catch (Exception e) {
      log.error("Failed to process order.created event: {}", e.toString(), e);

      // при ошибке тоже лучше пометить заказ как FAILED
      kafkaTemplate.send(orderFailedTopic, record.key(), record.value());
    }
  }
}
