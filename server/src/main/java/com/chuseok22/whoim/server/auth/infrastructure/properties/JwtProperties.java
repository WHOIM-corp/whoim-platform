package com.chuseok22.whoim.server.auth.infrastructure.properties;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

// HS512 서명 키는 64바이트 이상이어야 한다. refresh 관련 키는 바인딩하지 않는다
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secretKey, long accessExpMillis, String issuer) {

  private static final int MINIMUM_SECRET_KEY_BYTES = 64;

  public JwtProperties {
    if (secretKey == null || secretKey.isBlank()
        || secretKey.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_KEY_BYTES) {
      throw new IllegalStateException("jwt.secret-key는 최소 64바이트여야 합니다");
    }
  }
}
