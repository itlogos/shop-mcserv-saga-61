package com.example.order.web;

import com.example.order.model.Order;
import com.example.order.repo.OrderRepository;
import com.example.order.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService service;
    private final OrderRepository repo;

    public OrderController(OrderService service, OrderRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    // Получить все заказы или только по customerId
    @GetMapping("/orders")
    public List<Order> all(@RequestParam(value = "customerId", required = false) Long customerId) {
        if (customerId != null) {
            return repo.findByCustomerId(customerId);
        }
        return repo.findAll();
    }

    @PostMapping("/orders")
    public Order create(@RequestBody Order o) {
        return service.create(o);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> one(@PathVariable("id") Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Возврат 1 шт по позиции заказа
    @PostMapping("/orders/{orderId}/items/{productId}/return-one")
    public ResponseEntity<Order> returnOne(
            @PathVariable Long orderId,
            @PathVariable Long productId) {

        try {
            Order updated = service.returnOne(orderId, productId);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
