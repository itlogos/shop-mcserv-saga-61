package com.example.customer.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  @ConditionalOnProperty(prefix="app", name="securityDisabled", havingValue="true")
  SecurityFilterChain insecure(HttpSecurity http) throws Exception {
    http.csrf(csrf->csrf.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll());
    return http.build();
  }
  @Bean
  @ConditionalOnProperty(prefix="app", name="securityDisabled", havingValue="false", matchIfMissing=true)
  SecurityFilterChain secure(HttpSecurity http) throws Exception {
    http.csrf(csrf->csrf.disable())
      .authorizeHttpRequests(a->a
        .requestMatchers("/actuator/**").permitAll()
        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN","CUSTOMER")
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated())
      .oauth2ResourceServer(o->o.jwt(Customizer.withDefaults()));
    return http.build();
  }
}
