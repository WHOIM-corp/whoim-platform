package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
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
}
