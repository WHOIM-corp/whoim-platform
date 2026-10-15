package com.chuseok22.whoim.server.config;

import com.chuseok22.whoim.server.auth.infrastructure.config.SecurityPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  // formLogin·httpBasic은 JWT 인증(#2)으로 교체될 임시 설정이다
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(SecurityPaths.PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
