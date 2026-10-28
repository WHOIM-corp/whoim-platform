package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.core.constant.PromptCommonRules;
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

  @Test
  void build_wrapsInputInSentinel() {
    String prompt = GapAnalysisPrompt.build("지침", "[D1:L1] 내용").prompt();

    assertThat(prompt).isEqualTo("다음 업무 기록에서 Gap을 찾으세요.\n\n" + PromptCommonRules.wrapInputData("[D1:L1] 내용"));
    assertThat(prompt).contains("===== BEGIN INPUT DATA =====\n\n[D1:L1] 내용\n\n===== END INPUT DATA =====");
  }

  @Test
  void build_includesCommonRulesBetweenInstructionAndFormat() {
    String system = GapAnalysisPrompt.build("지침", "입력").system();

    assertThat(system).startsWith("지침\n\n" + PromptCommonRules.COMMON_RULES + "\n\n- 반드시 JSON만 출력합니다.");
  }

  @Test
  void build_formatBlockDescribesInputLinesAndEmptyGaps() {
    String system = GapAnalysisPrompt.build("지침", "입력").system();

    assertThat(system).contains("Gap이 없으면 gaps는 빈 배열입니다", "`[D1:L3] 내용`", "## D1 파일명 (유형)", "\"anchors\"");
  }
}
