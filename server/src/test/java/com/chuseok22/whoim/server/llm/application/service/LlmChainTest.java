package com.chuseok22.whoim.server.llm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LlmChainTest {

  record TestAnswer(@NotBlank String answer) {
  }

  private final LlmRequest request = new LlmRequest(null, "질문", null, null);
  private LLMProvider first;
  private LLMProvider second;
  private LlmChain chain;

  @BeforeEach
  void setUp() {
    first = mock(LLMProvider.class);
    second = mock(LLMProvider.class);
    chain = new LlmChain(List.of(first, second), JsonMapper.builder().build(),
        Validation.buildDefaultValidatorFactory().getValidator());
  }

  private static void respondWith(LLMProvider provider, String content) {
    when(provider.generate(any())).thenReturn(new LlmResponse(content, "test"));
  }

  private static void assertLlmUnavailable(Throwable thrown) {
    assertThat(thrown).isInstanceOfSatisfying(CustomException.class, e -> {
      assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
      assertThat(e.getCause()).isNull();
    });
  }

  @Test
  void generate_parsesContentIntoRecord() {
    respondWith(first, "{\"answer\":\"ok\"}");

    assertThat(chain.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void generate_extractsJsonFromWrappedContent() {
    String[] wrappedContents = {
        "```json\n{\"answer\":\"ok\"}\n```",
        "<think>생각</think>{\"answer\":\"ok\"}",
        "<think>초안 {\"answer\":\"x\"}</think>{\"answer\":\"ok\"}"
    };

    for (String content : wrappedContents) {
      respondWith(first, content);
      assertThat(chain.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
    }
  }

  @Test
  void generate_ignoresUnknownFields() {
    respondWith(first, "{\"answer\":\"ok\",\"extra\":1}");

    assertThat(chain.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void generate_throwsLlmUnavailable_whenContentNotJson() {
    respondWith(first, "죄송합니다");
    respondWith(second, "죄송합니다");

    // 파서 메시지가 응답 본문 토큰을 담을 수 있어 cause를 남기지 않는다
    assertThatThrownBy(() -> chain.generate(request, TestAnswer.class))
        .satisfies(LlmChainTest::assertLlmUnavailable);
  }

  @Test
  void generate_throwsLlmUnavailable_whenContentIsNullLiteral() {
    respondWith(first, "null");
    respondWith(second, "null");

    assertThatThrownBy(() -> chain.generate(request, TestAnswer.class))
        .satisfies(LlmChainTest::assertLlmUnavailable);
  }

  @Test
  void generate_throwsLlmUnavailable_whenValidationFails() {
    respondWith(first, "{\"answer\":\"\"}");
    respondWith(second, "{\"answer\":\"\"}");

    assertThatThrownBy(() -> chain.generate(request, TestAnswer.class))
        .satisfies(LlmChainTest::assertLlmUnavailable);
  }

  @Test
  void usesSecondProvider_whenFirstThrowsLlmUnavailable() {
    when(first.generate(any())).thenThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));
    respondWith(second, "{\"answer\":\"ok\"}");

    assertThat(chain.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void usesSecondProvider_whenFirstContentNotJson() {
    respondWith(first, "죄송합니다");
    respondWith(second, "{\"answer\":\"ok\"}");

    assertThat(chain.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void usesSecondProvider_whenFirstViolatesValidation() {
    respondWith(first, "{\"answer\":\"\"}");
    respondWith(second, "{\"answer\":\"ok\"}");

    assertThat(chain.generate(request, TestAnswer.class)).isEqualTo(new TestAnswer("ok"));
  }

  @Test
  void doesNotCallSecond_whenFirstSucceeds() {
    respondWith(first, "{\"answer\":\"ok\"}");

    chain.generate(request, TestAnswer.class);

    verify(second, never()).generate(any());
  }

  @Test
  void callsEachProviderOnce() {
    when(first.generate(any())).thenThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));
    when(second.generate(any())).thenThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    assertThatThrownBy(() -> chain.generate(request, TestAnswer.class))
        .isInstanceOf(CustomException.class);

    verify(first, times(1)).generate(any());
    verify(second, times(1)).generate(any());
  }

  @Test
  void throwsLlmUnavailable_whenAllProvidersFail() {
    when(first.generate(any())).thenThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));
    when(second.generate(any())).thenThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    assertThatThrownBy(() -> chain.generate(request, TestAnswer.class))
        .satisfies(LlmChainTest::assertLlmUnavailable);
  }

  @Test
  void propagatesOtherCustomException() {
    CustomException failure = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
    when(first.generate(any())).thenThrow(failure);

    assertThatThrownBy(() -> chain.generate(request, TestAnswer.class)).isSameAs(failure);
    verify(second, never()).generate(any());
  }

  @Test
  void appliesContentTransformerBeforeParsing() {
    respondWith(first, "{\"answer\":\"TOKEN\"}");

    TestAnswer answer = chain.generate(request, TestAnswer.class,
        content -> content.replace("TOKEN", "restored"));

    assertThat(answer).isEqualTo(new TestAnswer("restored"));
  }

  @Test
  void appliesTransformerPerProvider() {
    respondWith(first, "죄송합니다");
    respondWith(second, "{\"answer\":\"TOKEN\"}");

    TestAnswer answer = chain.generate(request, TestAnswer.class,
        content -> content.replace("TOKEN", "restored"));

    assertThat(answer).isEqualTo(new TestAnswer("restored"));
  }
}
