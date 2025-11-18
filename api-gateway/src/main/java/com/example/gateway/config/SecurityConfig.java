package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain springSecuritylterChain(ServerHttpSecurity http) {
    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // Включаем CORS, чтобы использовался spring.cloud.gateway.globalcors из application.yml
            .cors(Customizer.withDefaults())
            .authorizeExchange(ex -> ex
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()

                    // Публично – витрина товаров
                    .pathMatchers(HttpMethod.GET, "/store/api/products").permitAll()

                    // Админка магазина
                    .pathMatchers("/store/api/admin/**").hasRole("ADMIN")

                    // Бизнес-операции магазина (просмотр/покупка, кроме админки)
                    .pathMatchers("/store/**").hasAnyRole("CUSTOMER", "ADMIN")

                    // Клиенты
                    .pathMatchers("/customer/**").hasAnyRole("CUSTOMER", "ADMIN")

                    // Заказы
                    .pathMatchers("/order/**").hasAnyRole("CUSTOMER", "ADMIN")

                    // На всякий случай – всё остальное только для аутентифицированных
                    .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveKeycloakRealmRoleConverter()))
            )
            .build();
  }

  /**
   * Конвертер ролей из Keycloak (realm_access.roles -> ROLE_xxx)
   * Оригинальная логика из твоего проекта.
   */
  private Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveKeycloakRealmRoleConverter() {
    JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
    delegate.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
      Map<String, Object> ra = jwt.getClaimAsMap("realm_access");
      Object roles = (ra != null) ? ra.get("roles") : null;

      if (roles instanceof Collection<?> col) {
        return col.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r))
                .collect(Collectors.toList()); // List<GrantedAuthority>
      }
      return List.<GrantedAuthority>of();
    });
    return new ReactiveJwtAuthenticationConverterAdapter(delegate);
  }
}

//

