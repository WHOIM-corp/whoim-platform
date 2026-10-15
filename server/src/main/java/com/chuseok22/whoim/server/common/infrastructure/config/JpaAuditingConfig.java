package com.chuseok22.whoim.server.common.infrastructure.config;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

  // Auditing 시각도 Clock 빈을 따르게 해 테스트에서 고정할 수 있게 한다
  @Bean
  public DateTimeProvider auditingDateTimeProvider(Clock clock) {
    return () -> Optional.of(Instant.now(clock));
  }
}
