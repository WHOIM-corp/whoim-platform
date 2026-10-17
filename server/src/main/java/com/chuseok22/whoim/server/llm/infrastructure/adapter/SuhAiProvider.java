package com.chuseok22.whoim.server.llm.infrastructure.adapter;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import com.chuseok22.whoim.server.llm.application.service.LLMProvider;
import com.chuseok22.whoim.server.llm.infrastructure.properties.SuhAiProperties;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class SuhAiProvider implements LLMProvider {

  private static final String PROVIDER_NAME = "SUH_AI";

  private final RestClient restClient;
  private final SuhAiProperties properties;
  private final SettingConfigService settingConfigService;

  // Lombok 설정에 @Qualifier 복사가 없어 같은 타입 빈과 구분하도록 생성자를 직접 쓴다
  public SuhAiProvider(
      @Qualifier("suhAiRestClient") RestClient restClient,
      SuhAiProperties properties,
      SettingConfigService settingConfigService
  ) {
    this.restClient = restClient;
    this.properties = properties;
    this.settingConfigService = settingConfigService;
  }

  @Override
  public LlmResponse generate(LlmRequest request) {
    // 모델은 이 한 지점에서만 읽는다. 관리자 기능이 생기면 이 지점만 바꾸면 된다
    String model = settingConfigService.getString(SettingKey.LLM_SUH_AI_MODEL, properties.model());

    ChatResult result;
    try {
      result = restClient.post()
          .uri("/ollama/chat")
          .body(buildRequestBody(request, model))
          .retrieve()
          .body(ChatResult.class);
    } catch (ResourceAccessException exception) {
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE, exception);
    } catch (RestClientException exception) {
      // 역직렬화 실패 메시지가 응답 본문 일부를 담을 수 있어 cause를 남기지 않는다
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }

    if (result == null
        || !Boolean.TRUE.equals(result.success())
        || result.error() != null
        || result.content() == null
        || result.content().isBlank()) {
      // 오류 본문이 예외나 로그로 새지 않도록 원인 없이 실패만 알린다
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }

    logSuccess(result.model() != null ? result.model() : model, result.metrics());
    return new LlmResponse(result.content(), PROVIDER_NAME);
  }

  private Map<String, Object> buildRequestBody(LlmRequest request, String model) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("prompt", request.prompt());
    if (request.system() != null) {
      body.put("system", request.system());
    }
    body.put("temperature", resolveTemperature(request));
    body.put("auto_unload", false);
    body.put("format", request.responseSchema() != null ? request.responseSchema() : "json");
    return body;
  }

  private double resolveTemperature(LlmRequest request) {
    if (request.temperature() != null) {
      return request.temperature();
    }
    return settingConfigService.getDouble(SettingKey.LLM_SUH_AI_TEMPERATURE);
  }

  private void logSuccess(String model, Map<String, Object> metrics) {
    Map<String, Object> safeMetrics = metrics != null ? metrics : Map.of();
    log.info("LLM 호출 성공: provider={}, model={}, totalDurationMs={}, promptEvalCount={}, evalCount={}",
        PROVIDER_NAME, model,
        safeMetrics.get("total_duration_ms"),
        safeMetrics.get("prompt_eval_count"),
        safeMetrics.get("eval_count"));
  }

  // metrics는 snake_case 키가 가변적이라 Map으로 받는다
  private record ChatResult(
      String content,
      Map<String, Object> metrics,
      String model,
      Boolean success,
      String error
  ) {
  }
}
