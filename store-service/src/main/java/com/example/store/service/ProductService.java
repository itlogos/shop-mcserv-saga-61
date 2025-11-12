package com.example.store.service;

import com.example.store.model.Product;
import com.example.store.repo.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public List<Product> findAll() {
        return repo.findAll();
    }

    public Product findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product %d not found".formatted(id)));
    }

    @Transactional
    public Product create(Product p) {
        if (p.getQuantity() == null) {
            p.setQuantity(0);
        }
        return repo.save(p);
    }

    @Transactional
    public Product update(Long id, Product p) {
        Product existing = findById(id);
        if (p.getName() != null) existing.setName(p.getName());
        if (p.getPrice() != null) existing.setPrice(p.getPrice());
        if (p.getQuantity() != null) existing.setQuantity(p.getQuantity());
        return repo.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

}
