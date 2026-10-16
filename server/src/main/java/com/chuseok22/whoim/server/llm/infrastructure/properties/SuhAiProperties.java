package com.chuseok22.whoim.server.llm.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 값이 비면 기동 단계에서 실패시킨다. 메시지에는 키 이름만 담아 값이 노출되지 않게 한다
@ConfigurationProperties(prefix = "llm.suh-ai")
public record SuhAiProperties(String baseUrl, String apiKey, String model) {

  public SuhAiProperties {
    requireText(baseUrl, "llm.suh-ai.base-url");
    requireText(apiKey, "llm.suh-ai.api-key");
    requireText(model, "llm.suh-ai.model");
    // 끝의 '/'를 남기면 요청 경로가 '//ollama/chat'이 된다
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  // 기본 toString은 모든 컴포넌트를 출력하므로 로그·예외 메시지로 API 키가 새지 않게 마스킹한다
  @Override
  public String toString() {
    return "SuhAiProperties[baseUrl=" + baseUrl + ", apiKey=****, model=" + model + "]";
  }

  private static void requireText(String value, String key) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(key + "가 필요합니다");
    }
  }
}
