package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import org.junit.jupiter.api.Test;

class DlpDetectionPromptTest {

  @Test
  void build_appendsFormatBlock_toCustomInstruction() {
    LlmRequest request = DlpDetectionPrompt.build("커스텀", "입력");

    assertThat(request.system()).startsWith("커스텀\n\n")
        .contains("JSON만 출력", "\"detections\"", "CUSTOMER", "PARTNER", "PROJECT", "PERSON", "SECRET", "토큰");
    assertThat(request.responseSchema()).isNull();
    assertThat(request.temperature()).isNull();
  }

  @Test
  void build_appendsFormatBlock_whenInstructionIsEmptyString() {
    LlmRequest request = DlpDetectionPrompt.build("", "입력");

    assertThat(request.system()).contains("JSON만 출력", "\"detections\"");
  }

  @Test
  void build_putsOnlyMaskedTextInUserPrompt() {
    LlmRequest request = DlpDetectionPrompt.build(DefaultPromptInstructions.DLP_DETECTION, "마스킹된 [PERSON_01] 텍스트");

    assertThat(request.prompt()).isEqualTo("마스킹된 [PERSON_01] 텍스트");
  }

  @Test
  void build_doesNotApplyCommonRulesOrSentinel() {
    LlmRequest request = DlpDetectionPrompt.build(DefaultPromptInstructions.DLP_DETECTION, "마스킹된 텍스트");

    assertThat(request.system()).doesNotContain("INPUT DATA");
    assertThat(request.prompt()).doesNotContain("INPUT DATA").isEqualTo("마스킹된 텍스트");
  }

  @Test
  void build_formatBlockExcludesCardLabelsAndForbidsExtraFields() {
    LlmRequest request = DlpDetectionPrompt.build("커스텀", "입력");

    assertThat(request.system()).contains("`[C1]`, `[C2]`", "지정된 필드 외의 필드는 추가하지 않습니다.",
        "{\"detections\":[]}");
  }

  @Test
  void defaultInstruction_isSpecP5() {
    assertThat(SettingKey.PROMPT_DLP_DETECTION.defaultValue()).isEqualTo(DefaultPromptInstructions.DLP_DETECTION);
    assertThat(DefaultPromptInstructions.DLP_DETECTION)
        .contains("CUSTOMER:", "PARTNER:", "PROJECT:", "PERSON:", "SECRET:", "판단 기준:")
        .doesNotEndWith("\n");
  }
}
