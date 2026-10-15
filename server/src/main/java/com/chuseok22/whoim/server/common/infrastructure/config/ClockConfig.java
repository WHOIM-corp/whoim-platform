package com.chuseok22.whoim.server.common.infrastructure.config;

import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  // 현재 시각을 직접 호출하지 않고 이 빈을 주입받아 테스트에서 고정할 수 있게 한다
  @Bean
  public Clock clock() {
    return Clock.system(SeoulTime.ZONE);
  }
}
