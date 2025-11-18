package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(Authentication.class)
                .map(auth -> {
                    // роли из JWT (уже сконвертированы в ROLE_xxx)
                    boolean isAdmin = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(a -> a.equals("ROLE_ADMIN"));

                    // sub как идентификатор пользователя (можно достать из principal или из Jwt в другом месте)
                    String principalName = auth.getName(); // по умолчанию sub

                    // Можно зашить роль в ключ (для мониторинга)
                    if (isAdmin) {
                        return "admin:" + principalName;
                    } else {
                        return "customer:" + principalName;
                    }
                })
                // если не залогинен – лимитируем по IP
                .defaultIfEmpty(
                        "anon:" + Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                .map(addr -> addr.getAddress().getHostAddress())
                                .orElse("unknown")
                );
    }
}
