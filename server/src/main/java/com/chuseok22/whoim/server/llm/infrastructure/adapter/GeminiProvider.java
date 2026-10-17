package com.chuseok22.whoim.server.llm.infrastructure.adapter;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import com.chuseok22.whoim.server.llm.application.service.LLMProvider;
import com.chuseok22.whoim.server.llm.infrastructure.properties.GeminiProperties;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeminiProvider implements LLMProvider {

  private static final String PROVIDER_NAME = "GEMINI";

  private final RestClient restClient;
  private final GeminiProperties properties;
  private final SettingConfigService settingConfigService;

  // Lombok 설정에 @Qualifier 복사가 없어 같은 타입 빈과 구분하도록 생성자를 직접 쓴다
  public GeminiProvider(
      @Qualifier("geminiRestClient") RestClient restClient,
      GeminiProperties properties,
      SettingConfigService settingConfigService
  ) {
    this.restClient = restClient;
    this.properties = properties;
    this.settingConfigService = settingConfigService;
  }

  @Override
  public LlmResponse generate(LlmRequest request) {
    String model = settingConfigService.getString(SettingKey.LLM_GEMINI_MODEL, properties.model());

    GenerateResult result;
    try {
      result = restClient.post()
          .uri("/models/{model}:generateContent", model)
          .body(buildRequestBody(request))
          .retrieve()
          .body(GenerateResult.class);
    } catch (ResourceAccessException exception) {
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE, exception);
    } catch (RestClientException exception) {
      // 역직렬화 실패 메시지가 응답 본문 일부를 담을 수 있어 cause를 남기지 않는다
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }

    String text = extractText(result);
    if (text.isBlank()) {
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }

    log.info("LLM 호출 성공: provider={}, model={}", PROVIDER_NAME, model);
    return new LlmResponse(text, PROVIDER_NAME);
  }

  private Map<String, Object> buildRequestBody(LlmRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", request.prompt())))));
    if (request.system() != null) {
      body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.system()))));
    }
    // 스키마 필드는 보내지 않는다. 응답 검증은 서버가 한다
    body.put("generationConfig", Map.of(
        "responseMimeType", "application/json",
        "temperature", resolveTemperature(request)));
    return body;
  }

  private double resolveTemperature(LlmRequest request) {
    if (request.temperature() != null) {
      return request.temperature();
    }
    return settingConfigService.getDouble(SettingKey.LLM_GEMINI_TEMPERATURE);
  }

  // 차단·후보 없음·parts 없음은 모두 빈 문자열이 되어 호출부에서 실패로 처리된다
  private String extractText(GenerateResult result) {
    if (result == null
        || (result.promptFeedback() != null && result.promptFeedback().blockReason() != null)
        || result.candidates() == null
        || result.candidates().isEmpty()) {
      return "";
    }
    Candidate candidate = result.candidates().get(0);
    if (candidate == null || candidate.content() == null || candidate.content().parts() == null) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    candidate.content().parts().stream()
        .filter(Objects::nonNull)
        .filter(part -> !Boolean.TRUE.equals(part.thought()) && part.text() != null)
        .forEach(part -> text.append(part.text()));
    return text.toString();
  }

  private record GenerateResult(List<Candidate> candidates, PromptFeedback promptFeedback) {
  }

  private record Candidate(Content content) {
  }

  private record Content(List<Part> parts) {
  }

  private record Part(String text, Boolean thought) {
  }

  private record PromptFeedback(String blockReason) {
  }
}
