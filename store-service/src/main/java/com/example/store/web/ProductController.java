package com.example.store.web;
import com.example.store.model.Product;
import com.example.store.service.ProductService;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {
  private final ProductService service;
  public ProductController(ProductService service){ this.service = service; }

  @GetMapping("/products")
  public List<Product> all(){ return service.findAll(); }

  @PostMapping("/admin/products")
  public Product create(@RequestBody Product p){ return service.create(p); }

  @DeleteMapping("/admin/products/{id}")
  public ResponseEntity<?> delete(@PathVariable Long id){ service.delete(id); return ResponseEntity.noContent().build(); }

  }
