package com.example.customer.web;

import com.example.customer.model.Customer;
import com.example.customer.repo.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerRepository repo;

    public CustomerController(CustomerRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/customers")
    public List<Customer> all() {
        return repo.findAll();
    }

    @GetMapping("/customers/{id}")
    public Customer byId(@PathVariable("id") @Min(1) Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer %d not found".formatted(id)));
    }

    @PostMapping("/admin/customers")
    public Customer create(@RequestBody Customer c) {
        return repo.save(c);
    }

    @DeleteMapping("/admin/customers/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") @Min(1) Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Customer %d not found".formatted(id));
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
