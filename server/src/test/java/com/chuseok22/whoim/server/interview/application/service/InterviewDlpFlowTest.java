package com.chuseok22.whoim.server.interview.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.common.application.text.ExcerptMatcher;
import com.chuseok22.whoim.server.dlp.application.service.DlpAiDetector;
import com.chuseok22.whoim.server.dlp.application.service.DlpRuleMasker;
import com.chuseok22.whoim.server.dlp.application.service.DlpService;
import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.dlp.infrastructure.properties.DlpProperties;
import com.chuseok22.whoim.server.interview.application.dto.data.InterviewTurnResult;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;
import com.chuseok22.whoim.server.llm.application.service.LlmChain;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import jakarta.validation.Validation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// 마스킹된 답변으로 생성해도 복원 후 발췌가 원문 답변에서 검증되는지(DIRECT 유지) 확인한다
class InterviewDlpFlowTest {

  private static final String ANSWER = "김가나 팀장이 먼저 전화했다";

  private final List<LlmRequest> sentRequests = new ArrayList<>();
  private final DlpAiDetector detector = mock(DlpAiDetector.class);
  private final LlmService llmService = new LlmService(
      new LlmChain(List.of(request -> {
        sentRequests.add(request);
        return new LlmResponse("""
            {"extracted":[{"element":"SIGNAL","status":"FILLED","content":"선행 연락",
            "source":"DIRECT","excerpt":"[PERSON_01] 팀장이 먼저 전화했다"}],"nextQuestion":"다음은?"}""",
            "fake");
      }), JsonMapper.builder().build(), Validation.buildDefaultValidatorFactory().getValidator()),
      new DlpService(new DlpRuleMasker(new DlpProperties(Map.of(DlpEntityType.PERSON, List.of("김가나")))), detector));

  private InterviewTurnResult generateFromAnswer() {
    when(detector.detect(anyString())).thenReturn(List.of());
    return llmService.generate(new LlmRequest(null, "답변: " + ANSWER, null, null), InterviewTurnResult.class);
  }

  @Test
  void interviewTurn_keepsDirectEvidence_afterRestore() {
    InterviewTurnResult result = generateFromAnswer();

    assertThat(sentRequests.get(0).prompt()).doesNotContain("김가나").contains("[PERSON_01]");
    Map<TraceElement, TraceStateEntry> accepted = new TraceExtractionValidator()
        .accept(result.extracted(), ANSWER, List.of(), 1);
    assertThat(accepted.get(TraceElement.SIGNAL).status()).isEqualTo(TraceElementStatus.FILLED);
  }

  @Test
  void cardDirectEvidence_isFoundInOriginalAnswer_afterRestore() {
    InterviewTurnResult result = generateFromAnswer();

    String excerpt = result.extracted().get(0).excerpt();

    assertThat(excerpt).isEqualTo("김가나 팀장이 먼저 전화했다");
    assertThat(ExcerptMatcher.findIn(ANSWER, excerpt)).isPresent();
  }
}
