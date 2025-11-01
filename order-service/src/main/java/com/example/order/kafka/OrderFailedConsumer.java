package com.example.order.kafka;
import com.example.order.repo.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderFailedConsumer {
  private static final Logger log = LoggerFactory.getLogger(OrderFailedConsumer.class);
  private final OrderRepository repo;
  public OrderFailedConsumer(OrderRepository repo){ this.repo = repo; }

  @KafkaListener(topics = "${app.orderFailedTopic:order.failed}", groupId = "order-service")
  @Transactional
  public void onMessage(ConsumerRecord<String,String> rec){
    try {
      Long id = Long.valueOf(rec.key());
      repo.findById(id).ifPresent(o -> { o.setStatus("FAILED"); repo.save(o); });
      log.info("Order {} marked FAILED", id);
    } catch (Exception e){
      log.warn("Failed to process failure: {}", e.toString());
    }
  }
}
