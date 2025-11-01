package com.example.order.web;
import com.example.order.model.Order;
import com.example.order.repo.OrderRepository;
import com.example.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class OrderController {
  private final OrderService service; private final OrderRepository repo;
  public OrderController(OrderService service, OrderRepository repo){ this.service = service; this.repo = repo; }

  @GetMapping("/orders") public List<Order> all(){ return repo.findAll(); }
  @PostMapping("/orders") public Order create(@RequestBody Order o){ return service.create(o); }
  @GetMapping("/orders/{id}") public ResponseEntity<Order> one(@PathVariable Long id){ return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); }
}
