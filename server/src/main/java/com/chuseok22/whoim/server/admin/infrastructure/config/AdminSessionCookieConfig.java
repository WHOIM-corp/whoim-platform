package com.chuseok22.whoim.server.admin.infrastructure.config;

import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminSessionCookieConfig {

  private static final String SESSION_COOKIE_NAME = "JSESSIONID";

  // application-*.yml을 수정하지 않으려고 빈으로 SameSite를 지정한다. HttpOnly는 Tomcat 기본값이다
  @Bean
  public CookieSameSiteSupplier adminSessionCookieSameSiteSupplier() {
    return CookieSameSiteSupplier.ofLax().whenHasName(SESSION_COOKIE_NAME);
  }
}
