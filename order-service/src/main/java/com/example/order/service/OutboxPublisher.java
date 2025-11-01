package com.example.order.service;
import com.example.order.model.OutboxEvent;
import com.example.order.repo.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxRepository repo;
  private final KafkaTemplate<String,String> kafka;
  private final String topic;

  public OutboxPublisher(OutboxRepository repo, KafkaTemplate<String,String> kafka,
                         @Value("${app.orderTopic:order.created}") String topic){
    this.repo = repo; this.kafka = kafka; this.topic = topic;
  }

  @Scheduled(fixedDelayString = "${app.outbox.interval.ms:2000}")
  @Transactional
  public void publish(){
    List<OutboxEvent> events = repo.findUnpublished();
    for (OutboxEvent e : events){
      try {
        kafka.send(new ProducerRecord<>(topic, e.getAggregateId(), e.getPayload()));
        e.setPublishedAt(Instant.now());
        log.info("Outbox published id={} type={} aggregateId={}", e.getId(), e.getType(), e.getAggregateId());
      } catch (Exception ex){
        log.warn("Failed to publish outbox id={}: {}", e.getId(), ex.toString());
      }
    }
  }
}
