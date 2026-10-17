package com.chuseok22.whoim.server.llm.application.service;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class LlmChain {

  // thought 블록 안의 중괄호가 JSON 구간 추출을 흐리지 않도록 먼저 제거한다
  private static final Pattern THINK_BLOCK = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);
  private static final Pattern CODE_FENCE = Pattern.compile("```(?:json)?");

  private final List<LLMProvider> providers;
  private final JsonMapper jsonMapper;
  private final Validator validator;

  public LlmChain(List<LLMProvider> providers, JsonMapper jsonMapper, Validator validator) {
    this.providers = List.copyOf(providers);
    this.jsonMapper = jsonMapper;
    this.validator = validator;
  }

  public <T> T generate(LlmRequest request, Class<T> responseType) {
    return generate(request, responseType, UnaryOperator.identity());
  }

  // 같은 Provider는 재시도하지 않고 다음 Provider로 한 번씩만 넘어간다
  public <T> T generate(LlmRequest request, Class<T> responseType, UnaryOperator<String> contentTransformer) {
    for (LLMProvider provider : providers) {
      try {
        return generateWith(provider, request, responseType, contentTransformer);
      } catch (CustomException exception) {
        if (exception.getErrorCode() != ErrorCode.LLM_UNAVAILABLE) {
          throw exception;
        }
        log.warn("LLM Provider 실패, 다음 Provider로 넘어간다: provider={}", provider.getClass().getSimpleName());
      }
    }
    throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
  }

  private <T> T generateWith(
      LLMProvider provider,
      LlmRequest request,
      Class<T> responseType,
      UnaryOperator<String> contentTransformer
  ) {
    LlmResponse response = provider.generate(request);
    T parsed = parse(response, contentTransformer.apply(response.content()), responseType);
    if (parsed == null) {
      log.warn("LLM 응답 검증 실패: provider={}, reason=null", response.providerName());
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }
    Set<ConstraintViolation<T>> violations = validator.validate(parsed);
    if (!violations.isEmpty()) {
      log.warn("LLM 응답 검증 실패: provider={}, reason=validation", response.providerName());
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }
    return parsed;
  }

  private <T> T parse(LlmResponse response, String content, Class<T> responseType) {
    try {
      return jsonMapper.readValue(extractJson(content), responseType);
    } catch (JacksonException e) {
      // 파서 메시지가 응답 본문 토큰을 담을 수 있어 cause 없이 예외 종류만 남긴다
      log.warn("LLM 응답 파싱 실패: provider={}, reason=parse, error={}",
          response.providerName(), e.getClass().getSimpleName());
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }
  }

  private String extractJson(String content) {
    String stripped = CODE_FENCE.matcher(THINK_BLOCK.matcher(content).replaceAll("")).replaceAll("");
    int start = stripped.indexOf('{');
    int end = stripped.lastIndexOf('}');
    // 구간이 없으면 원문을 그대로 넘겨 파서가 실패하게 한다
    return start >= 0 && end > start ? stripped.substring(start, end + 1) : stripped;
  }
}
