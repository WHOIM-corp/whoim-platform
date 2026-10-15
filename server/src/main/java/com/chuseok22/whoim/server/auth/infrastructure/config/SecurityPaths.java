package com.chuseok22.whoim.server.auth.infrastructure.config;

public final class SecurityPaths {

  // CD 헬스체크와 API 문서는 인증 없이 열어 둔다. /error는 Spring Security가 ERROR 디스패치도 인가하므로 열지 않으면 401/403 응답 본문이 비게 된다
  public static final String[] PUBLIC_PATHS = {
      "/actuator/health",
      "/docs/swagger",
      "/swagger-ui/**",
      "/docs/swagger-ui/**",
      "/v3/api-docs/**",
      "/error"
  };

  private SecurityPaths() {
  }
}
