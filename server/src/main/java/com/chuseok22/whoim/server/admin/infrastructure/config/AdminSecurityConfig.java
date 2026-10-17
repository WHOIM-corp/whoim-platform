package com.chuseok22.whoim.server.admin.infrastructure.config;

import com.chuseok22.whoim.server.admin.application.service.AdminUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfig {

  private static final String ADMIN_PATH = "/api/admin/**";
  private static final String LOGIN_PATH = "/api/admin/login";
  private static final String LOGOUT_PATH = "/api/admin/logout";
  private static final String ASSET_PATH = "/api/admin/assets/**";

  private final AdminUserDetailsService adminUserDetailsService;
  private final AdminLoginSuccessHandler adminLoginSuccessHandler;

  // 관리자 체인은 세션·CSRF를 쓰고 JWT 체인과 분리된다. 인증 제공자는 이 체인에만 등록해 /api/** 인증에 영향을 주지 않는다
  @Bean
  @Order(1)
  public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
    var provider = new DaoAuthenticationProvider(adminUserDetailsService);
    provider.setPasswordEncoder(passwordEncoder);

    http
        .securityMatcher(ADMIN_PATH)
        .authenticationProvider(provider)
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(LOGIN_PATH, ASSET_PATH).permitAll()
            .anyRequest().hasRole("ADMIN"))
        .formLogin(form -> form
            .loginPage(LOGIN_PATH)
            .loginProcessingUrl(LOGIN_PATH)
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(adminLoginSuccessHandler)
            .failureUrl(LOGIN_PATH + "?error")
            .permitAll())
        .logout(logout -> logout
            .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGOUT_PATH))
            .logoutSuccessUrl(LOGIN_PATH + "?logout"))
        // 세션이 만료된 htmx 요청은 302 로그인 HTML 대신 401과 HX-Redirect를 받아 화면 전체가 로그인으로 이동한다
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthenticatedEntryPoint()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        // 로그인 전 요청을 세션에 저장하지 않아 불필요한 세션 생성을 막는다
        .requestCache(cache -> cache.requestCache(new NullRequestCache()));
    return http.build();
  }

  private AuthenticationEntryPoint unauthenticatedEntryPoint() {
    AuthenticationEntryPoint htmxEntryPoint = (request, response, authException) -> {
      response.setHeader("HX-Redirect", LOGIN_PATH);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    };
    var delegating = new DelegatingAuthenticationEntryPoint(
        new LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>(
            Map.of(new RequestHeaderRequestMatcher("HX-Request", "true"), htmxEntryPoint)));
    delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_PATH));
    return delegating;
  }
}
