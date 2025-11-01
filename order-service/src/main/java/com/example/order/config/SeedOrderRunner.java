package com.example.order.config;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class SeedOrderRunner {
  private static final Logger log = LoggerFactory.getLogger(SeedOrderRunner.class);

  @Bean
  CommandLineRunner seedOrder(
      OrderService orderService,
      @Value("${app.seedOrder:false}") boolean seedOrderFlag
  ){
    return args -> {
      if (!seedOrderFlag) return;
      try {
        // delay to ensure store-service is up and seeded
        Thread.sleep(Duration.ofSeconds(20).toMillis());
        Order o = Order.builder()
            .customerId(1L)
            .items(List.of(
                OrderItem.builder().productId(1L).quantity(1).price(699.0).build(),
                OrderItem.builder().productId(3L).quantity(1).price(349.0).build()
            ))
            .status("NEW")
            .build();
        Order saved = orderService.create(o);
        log.info("Seed order created: {}", saved.getId());
      } catch (Exception e){
        log.warn("Seed order failed: {}", e.toString());
      }
    };
  }
}
