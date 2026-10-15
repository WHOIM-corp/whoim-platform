package com.chuseok22.whoim.server.auth.infrastructure.config;

import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.common.application.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    response.setStatus(ErrorCode.ACCESS_DENIED.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(ErrorCode.ACCESS_DENIED)));
  }
}
