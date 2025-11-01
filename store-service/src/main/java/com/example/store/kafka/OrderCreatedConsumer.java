package com.example.store.kafka;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.store.repo.ProductRepository;
import com.example.store.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;

@Component
@Transactional
public class OrderCreatedConsumer {

  private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

  private final KafkaTemplate<String, String> kafka;
  private final ProductRepository repo;
  private final ObjectMapper mapper = new ObjectMapper();

  public OrderCreatedConsumer(KafkaTemplate<String, String> kafka,
                              ProductRepository repo) {
    this.kafka = kafka;
    this.repo = repo;
  }

  @KafkaListener(topics = "${app.orderTopic:order.created}", groupId = "store-service")
  public void onMessage(ConsumerRecord<String, String> rec) {
    log.info("Consumed order event key={} value={}", rec.key(), rec.value());

    // TODO: при необходимости распарсить payload через mapper и обновить склад/товары через repo

    // Для примера подтверждаем заказ тем же payload:
    kafka.send("${app.orderConfirmedTopic:order.confirmed}", rec.key(), rec.value());
  }
}
