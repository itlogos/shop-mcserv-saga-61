package com.example.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()

                    // публичный каталог
                    .requestMatchers(HttpMethod.GET, "/api/products").permitAll()

                    // админские операции
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // всё остальное API – только аутентифицированный пользователь (CUSTOMER/ADMIN)
                    .requestMatchers("/api/**").hasAnyRole("CUSTOMER", "ADMIN")

                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRealmRoleConverter()))
            );

    return http.build();
  }

  private JwtAuthenticationConverter keycloakRealmRoleConverter() {
    var conv = new JwtAuthenticationConverter();
    conv.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
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
    return conv;
  }
}
