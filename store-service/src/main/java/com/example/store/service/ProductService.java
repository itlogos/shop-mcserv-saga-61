package com.example.store.service;
import com.example.store.model.Product;
import com.example.store.repo.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductService {
  private final ProductRepository repo;
  public ProductService(ProductRepository repo){ this.repo = repo; }

  public List<Product> findAll(){ return repo.findAll(); }

  @Transactional
  public Product create(Product p){
    if (p.getQuantity() == null) {
      p.setQuantity(0);
    }
    return repo.save(p); }


  @Transactional
  public void delete(Long id){ repo.deleteById(id); }

  }
