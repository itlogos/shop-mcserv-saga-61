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
import org.springframework.transaction.annotation.Transactional; // <-- ; был пропущен

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OutboxPublisher(OutboxRepository repo,
                           KafkaTemplate<String, String> kafka,
                           @Value("${app.orderTopic:order.created}") String topic) {
        this.repo = repo;
        this.kafka = kafka;
        this.topic = topic;
    }

    /**
     * Периодически выбираем необработанные события и публикуем их в Kafka.
     * После УСПЕШНОЙ отправки удаляем запись из outbox (publishedAt отсутствует).
     * Семантика: at-least-once.
     */
    @Scheduled(fixedDelayString = "${app.outbox.interval.ms:2000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = repo.findAllByOrderByIdAsc(); // вместо findUnpublished()
        for (OutboxEvent e : events) {
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(topic, e.getAggregateId(), e.getPayload());

                // Блокирующее ожидание подтверждения отправки (до 5 секунд)
                kafka.send(record).get(5, TimeUnit.SECONDS);

                // Успех: удаляем событие из outbox
                repo.deleteById(e.getId());

                log.info("Outbox published id={} type={} aggregateId={}",
                        e.getId(), e.getEventType(), e.getAggregateId());

            } catch (Exception ex) {
                // Ошибка отправки — не удаляем; событие останется для ретрая
                log.warn("Failed to publish outbox id={}: {}", e.getId(), ex.toString());
            }
        }
    }
}
