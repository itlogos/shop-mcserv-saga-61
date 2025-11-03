package com.example.store.kafka;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.beans.factory.annotation.Value;
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
  @Value("${app.orderConfirmedTopic:order.confirmed}")
  private String orderConfirmedTopic;

  public OrderCreatedConsumer(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  @KafkaListener(topics = "${app.orderTopic:order.created}", groupId = "store-service")
  public void onMessage(ConsumerRecord<String, String> rec) {
    log.info("Consumed order event key={} value={}", rec.key(), rec.value());

    // TODO: при необходимости распарсить payload через mapper и обновить склад/товары через repo

    // отправляем в РЕАЛЬНЫЙ топик, значение которого пришло из конфигурации
    kafka.send(orderConfirmedTopic, rec.key(), rec.value());
  }
}
