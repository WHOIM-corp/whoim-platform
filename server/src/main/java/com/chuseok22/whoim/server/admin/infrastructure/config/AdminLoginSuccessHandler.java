package com.chuseok22.whoim.server.admin.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class AdminLoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final int SESSION_TIMEOUT_SECONDS = 3600;
  private static final String SUCCESS_PATH = "/api/admin/tables";

  // yml을 건드리지 않으려고 세션 만료를 로그인 성공 시점에 지정한다
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    request.getSession().setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
    response.sendRedirect(request.getContextPath() + SUCCESS_PATH);
  }
}
