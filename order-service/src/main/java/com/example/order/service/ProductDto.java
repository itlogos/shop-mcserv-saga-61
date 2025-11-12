
package com.example.order.service;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, BigDecimal price, Integer quantity) {
}
