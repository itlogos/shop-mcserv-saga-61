package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class JwtAuthoritiesConfig {

  /**
   * ВАЖНО: для WebFlux возвращаем именно Converter<Jwt, Mono<AbstractAuthenticationToken>>.
   * Внутри используем servlet-конвертер + адаптер.
   */
  @Bean
  public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    JwtAuthenticationConverter standard = new JwtAuthenticationConverter();

    // Подмешиваем наши авторити к стандартным SCOPE_* (из 'scope'/'scp')
    standard.setJwtGrantedAuthoritiesConverter(jwt -> {
      JwtGrantedAuthoritiesConverter scope = new JwtGrantedAuthoritiesConverter();
      // делаем коллекцию изменяемой и null-safe
      Collection<GrantedAuthority> base =
              new ArrayList<>(Optional.ofNullable(scope.convert(jwt)).orElseGet(List::of));

      // 1) realm_access.roles
      Object realm = jwt.getClaim("realm_access");
      if (realm instanceof Map<?, ?> realmMap) {
        Object rolesObj = realmMap.get("roles");
        if (rolesObj instanceof Collection<?> roles) {
          base.addAll(toRoleAuthorities(roles));
        }
      }

      // 2) resource_access.<client>.roles (берём все клиенты)
      Object resource = jwt.getClaim("resource_access");
      if (resource instanceof Map<?, ?> clients) {
        for (Object v : clients.values()) {
          if (v instanceof Map<?, ?> clientMap) {
            Object rolesObj = clientMap.get("roles");
            if (rolesObj instanceof Collection<?> roles) {
              base.addAll(toRoleAuthorities(roles));
            }
          }
        }
      }

      return base;
    });

    // Адаптер превращает servlet-конвертер в реактивный Converter<Jwt, Mono<AbstractAuthenticationToken>>
    return new ReactiveJwtAuthenticationConverterAdapter(standard);
  }

  private Collection<GrantedAuthority> toRoleAuthorities(Collection<?> roles) {
    return roles.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
  }
}
