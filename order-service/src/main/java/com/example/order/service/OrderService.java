package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OutboxEvent;
import com.example.order.repo.OrderRepository;
import com.example.order.repo.OutboxRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

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

        // WebClient с автоматическим прокидыванием Bearer-токена
        this.storeClient = WebClient.builder()
                .baseUrl(storeUrl) // http://store-service:8081
                .filter((request, next) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        String tokenValue = jwtAuth.getToken().getTokenValue();

                        ClientRequest newRequest = ClientRequest.from(request)
                                .headers(headers -> headers.setBearerAuth(tokenValue))
                                .build();

                        return next.exchange(newRequest);
                    }

                    // если по какой-то причине аутентификации нет — идём без заголовка
                    return next.exchange(request);
                })
                .build();
    }

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
            // Embeddable + @ElementCollection – setOrder(order) не нужен
        }

        // 2) Статус
        order.setStatus("CREATED");

        // 3) Сохраняем заказ
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

    @Transactional
    public Order returnOne(Long orderId, Long productId) {
        Order order = repo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order %d not found".formatted(orderId)));

        boolean updated = false;

        var iterator = order.getItems().iterator();
        while (iterator.hasNext()) {
            OrderItem item = iterator.next();
            if (productId.equals(item.getProductId())) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new IllegalStateException(
                            "Nothing to return for product %d in order %d".formatted(productId, orderId));
                }
                int newQty = item.getQuantity() - 1;
                item.setQuantity(newQty);
                if (newQty == 0) {
                    iterator.remove(); // позиция исчезает из заказа
                }
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new EntityNotFoundException(
                    "Order %d has no item with product %d".formatted(orderId, productId));
        }

        // Здесь можно добавить outbox-событие "OrderReturned" при необходимости

        return repo.save(order);
    }
}
