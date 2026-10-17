package com.chuseok22.whoim.server.llm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.llm.application.dto.data.GuardedInput;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LlmServiceTest {

  record TestAnswer(String answer) {
  }

  private final LlmChain chain = mock(LlmChain.class);
  private final LlmInputGuard guard = mock(LlmInputGuard.class);
  private final LlmService service = new LlmService(chain, guard);

  @Test
  void generate_passesMaskedPromptToChain() {
    Map<String, Object> schema = Map.of("type", "object");
    UnaryOperator<String> restorer = UnaryOperator.identity();
    when(guard.guard("원문")).thenReturn(new GuardedInput("마스킹", restorer));
    TestAnswer expected = new TestAnswer("ok");
    when(chain.generate(any(LlmRequest.class), eq(TestAnswer.class), any())).thenReturn(expected);

    TestAnswer actual = service.generate(new LlmRequest("시스템", "원문", schema, 0.3), TestAnswer.class);

    assertThat(actual).isSameAs(expected);
    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(chain).generate(captor.capture(), eq(TestAnswer.class), any());
    assertThat(captor.getValue()).isEqualTo(new LlmRequest("시스템", "마스킹", schema, 0.3));
  }

  @Test
  void generate_passesRestorerAsContentTransformer() {
    UnaryOperator<String> restorer = text -> text.replace("[PERSON_01]", "김가나");
    when(guard.guard("원문")).thenReturn(new GuardedInput("마스킹", restorer));

    service.generate(new LlmRequest(null, "원문", null, null), TestAnswer.class);

    verify(chain).generate(any(LlmRequest.class), eq(TestAnswer.class), eq(restorer));
  }

  @Test
  void generate_doesNotCallChain_whenGuardThrows() {
    when(guard.guard("원문")).thenThrow(new CustomException(ErrorCode.DLP_UNAVAILABLE));

    assertThatThrownBy(() -> service.generate(new LlmRequest(null, "원문", null, null), TestAnswer.class))
        .isInstanceOf(CustomException.class)
        .extracting(thrown -> ((CustomException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.DLP_UNAVAILABLE);
    verifyNoInteractions(chain);
  }
}
