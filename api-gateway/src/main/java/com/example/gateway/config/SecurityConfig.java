package com.example.gateway.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration @EnableWebFluxSecurity
public class SecurityConfig {
  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http.csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(ex->ex
        .pathMatchers("/actuator/**").permitAll()
        .pathMatchers("/store/api/admin/**").hasRole("ADMIN")
        .pathMatchers("/store/**").hasAnyRole("ADMIN","CUSTOMER")
        .pathMatchers("/customer/api/admin/**").hasRole("ADMIN")
        .pathMatchers("/customer/**").hasAnyRole("ADMIN","CUSTOMER")
        .pathMatchers("/order/**").hasAnyRole("ADMIN","CUSTOMER")
        .anyExchange().authenticated())
      .oauth2ResourceServer(o->o.jwt(Customizer.withDefaults()));
    return http.build();
  }
}
