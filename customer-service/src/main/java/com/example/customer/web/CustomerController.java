package com.example.customer.web;
import com.example.customer.model.Customer;
import com.example.customer.repo.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class CustomerController {
  private final CustomerRepository repo;
  public CustomerController(CustomerRepository repo){ this.repo = repo; }
  @GetMapping("/customers") public List<Customer> all(){ return repo.findAll(); }
  @PostMapping("/admin/customers") public Customer create(@RequestBody Customer c){ return repo.save(c); }
  @DeleteMapping("/admin/customers/{id}") public ResponseEntity<?> delete(@PathVariable Long id){ repo.deleteById(id); return ResponseEntity.noContent().build(); }
}
