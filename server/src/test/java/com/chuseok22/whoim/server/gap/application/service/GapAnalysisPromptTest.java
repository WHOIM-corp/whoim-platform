package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import org.junit.jupiter.api.Test;

class GapAnalysisPromptTest {

  @Test
  void build_includesInputAndRequiredInstructions() {
    LlmRequest request = GapAnalysisPrompt.build(DefaultPromptInstructions.GAP_ANALYSIS, "[D1:L1] 내용");

    assertThat(request.prompt()).contains("[D1:L1] 내용");
    assertThat(request.system()).contains("JSON만 출력", "추측", "CHOICE_GAP", "EVIDENCE_CONFLICT", "ASSESSMENT_GAP", "최대 3개", "\"gaps\"");
    assertThat(request.responseSchema()).isNull();
    assertThat(request.temperature()).isNull();
  }

  @Test
  void build_appendsFormatBlock_toDefaultInstruction() {
    LlmRequest request = GapAnalysisPrompt.build(DefaultPromptInstructions.GAP_ANALYSIS, "입력");

    assertThat(request.system()).startsWith(DefaultPromptInstructions.GAP_ANALYSIS);
    assertThat(request.system()).contains("응답 형태:", "{\"gaps\":[{\"type\":\"CHOICE_GAP\"");
  }

  @Test
  void build_appendsFormatBlock_toCustomInstruction() {
    LlmRequest request = GapAnalysisPrompt.build("커스텀", "입력");

    assertThat(request.system()).startsWith("커스텀\n\n").contains("JSON만 출력", "응답 형태:", "\"gaps\"");
  }

  @Test
  void build_appendsFormatBlock_whenInstructionIsEmptyString() {
    LlmRequest request = GapAnalysisPrompt.build("", "입력");

    assertThat(request.system()).contains("JSON만 출력", "응답 형태:", "\"gaps\"");
  }

  @Test
  void build_userPromptIsIndependentOfInstruction() {
    assertThat(GapAnalysisPrompt.build("커스텀", "입력").prompt())
        .isEqualTo(GapAnalysisPrompt.build(DefaultPromptInstructions.GAP_ANALYSIS, "입력").prompt());
  }
}
