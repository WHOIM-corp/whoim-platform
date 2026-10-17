package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import org.junit.jupiter.api.Test;

class GapAnalysisPromptTest {

  @Test
  void build_includesInputAndRequiredInstructions() {
    LlmRequest request = GapAnalysisPrompt.build("[D1:L1] 내용");

    assertThat(request.prompt()).contains("[D1:L1] 내용");
    assertThat(request.system()).contains("JSON만 출력", "추측", "CHOICE_GAP", "EVIDENCE_CONFLICT", "ASSESSMENT_GAP", "최대 3개", "\"gaps\"");
    assertThat(request.responseSchema()).isNull();
    assertThat(request.temperature()).isNull();
  }
}
