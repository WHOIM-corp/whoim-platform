package com.chuseok22.whoim.server.llm.infrastructure.config;

import com.chuseok22.whoim.server.llm.application.service.LlmChain;
import com.chuseok22.whoim.server.llm.infrastructure.adapter.GeminiProvider;
import com.chuseok22.whoim.server.llm.infrastructure.adapter.SuhAiProvider;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class LlmChainConfig {

  // 생성은 Gemini만 쓴다
  @Bean
  public LlmChain generationChain(GeminiProvider geminiProvider, JsonMapper jsonMapper, Validator validator) {
    return new LlmChain(List.of(geminiProvider), jsonMapper, validator);
  }

  // DLP 탐지는 SUH AI를 먼저 쓰고 실패하면 Gemini로 넘어간다
  @Bean
  public LlmChain detectionChain(
      SuhAiProvider suhAiProvider,
      GeminiProvider geminiProvider,
      JsonMapper jsonMapper,
      Validator validator
  ) {
    return new LlmChain(List.of(suhAiProvider, geminiProvider), jsonMapper, validator);
  }
}
