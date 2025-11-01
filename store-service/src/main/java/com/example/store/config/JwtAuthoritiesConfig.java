package com.example.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class JwtAuthoritiesConfig {
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
    conv.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    return conv;
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt){
    JwtGrantedAuthoritiesConverter scope = new JwtGrantedAuthoritiesConverter();
    Collection<GrantedAuthority> base = scope.convert(jwt);

    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    List<String> roles = realmAccess != null ? (List<String>) realmAccess.getOrDefault("roles", List.of()) : List.of();
    List<GrantedAuthority> roleAuth = roles.stream()
        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    base.addAll(roleAuth);
    return base;
  }
}
