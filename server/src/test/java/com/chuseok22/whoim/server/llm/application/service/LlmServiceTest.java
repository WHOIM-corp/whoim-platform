package com.chuseok22.whoim.server.llm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LlmServiceTest {

  record TestAnswer(@NotBlank String answer) {
  }

  private final LlmRequest request = new LlmRequest(null, "질문", null, null);
  private LLMProvider provider;
  private LlmService llmService;

  @BeforeEach
  void setUp() {
    provider = mock(LLMProvider.class);
    llmService = new LlmService(provider, JsonMapper.builder().build(),
        Validation.buildDefaultValidatorFactory().getValidator());
  }

  private void respondWith(String content) {
    when(provider.generate(any())).thenReturn(new LlmResponse(content, "test"));
  }

  @Test
  void generate_parsesContentIntoRecord() {
    respondWith("{\"answer\":\"ok\"}");

    assertThat(llmService.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void generate_extractsJsonFromWrappedContent() {
    String[] wrappedContents = {
        "```json\n{\"answer\":\"ok\"}\n```",
        "<think>생각</think>{\"answer\":\"ok\"}",
        "<think>초안 {\"answer\":\"x\"}</think>{\"answer\":\"ok\"}"
    };

    for (String content : wrappedContents) {
      respondWith(content);
      assertThat(llmService.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
    }
  }

  @Test
  void generate_ignoresUnknownFields() {
    respondWith("{\"answer\":\"ok\",\"extra\":1}");

    assertThat(llmService.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void generate_throwsLlmUnavailable_whenContentNotJson() {
    respondWith("죄송합니다");

    // 파서 메시지가 응답 본문 토큰을 담을 수 있어 cause를 남기지 않는다
    assertThatThrownBy(() -> llmService.generate(request, TestAnswer.class))
        .isInstanceOfSatisfying(CustomException.class, e -> {
          assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
          assertThat(e.getCause()).isNull();
        });
  }

  @Test
  void generate_throwsLlmUnavailable_whenContentIsNullLiteral() {
    respondWith("null");

    assertThatThrownBy(() -> llmService.generate(request, TestAnswer.class))
        .isInstanceOfSatisfying(CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
  }

  @Test
  void generate_throwsLlmUnavailable_whenValidationFails() {
    respondWith("{\"answer\":\"\"}");

    assertThatThrownBy(() -> llmService.generate(request, TestAnswer.class))
        .isInstanceOfSatisfying(CustomException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
  }

  @Test
  void generate_propagatesProviderFailure() {
    CustomException failure = new CustomException(ErrorCode.LLM_UNAVAILABLE);
    when(provider.generate(any())).thenThrow(failure);

    assertThatThrownBy(() -> llmService.generate(request, TestAnswer.class)).isSameAs(failure);
    verify(provider, times(1)).generate(any());
  }
}
