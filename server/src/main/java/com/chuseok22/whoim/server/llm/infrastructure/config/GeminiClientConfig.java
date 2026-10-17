package com.chuseok22.whoim.server.llm.infrastructure.config;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.infrastructure.properties.GeminiProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClientConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  @Bean
  public RestClient geminiRestClient(GeminiProperties properties) {
    return configure(RestClient.builder(), properties).build();
  }

  // 어댑터 테스트가 MockRestServiceServer를 바인딩하기 전에 같은 설정을 재사용하도록 노출한다
  public static RestClient.Builder configure(RestClient.Builder builder, GeminiProperties properties) {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    requestFactory.setReadTimeout(READ_TIMEOUT);

    return builder
        .baseUrl(properties.baseUrl())
        .defaultHeader("x-goog-api-key", properties.apiKey())
        .requestFactory(requestFactory)
        // 기본 핸들러는 응답 본문 일부를 예외 메시지에 넣으므로, 본문 없이 도메인 예외로 바꾼다
        .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
          throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
        });
  }
}
