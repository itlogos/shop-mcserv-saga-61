package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                    .pathMatchers("/actuator/**").permitAll()

                    .pathMatchers(HttpMethod.POST,   "/store/api/admin/**").hasRole("ADMIN")
                    .pathMatchers(HttpMethod.PUT,    "/store/api/admin/**").hasRole("ADMIN")
                    .pathMatchers(HttpMethod.DELETE, "/store/api/admin/**").hasRole("ADMIN")

                    .pathMatchers("/store/**", "/customer/**", "/order/**").authenticated()
                    .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveKeycloakRealmRoleConverter()))
            );

    return http.build();
  }

  private Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveKeycloakRealmRoleConverter() {
    var delegate = new JwtAuthenticationConverter();
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
