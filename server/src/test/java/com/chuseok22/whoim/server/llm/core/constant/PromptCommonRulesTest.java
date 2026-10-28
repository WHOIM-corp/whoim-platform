package com.chuseok22.whoim.server.llm.core.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptCommonRulesTest {

  @Test
  void commonRules_containsFourRules() {
    assertThat(PromptCommonRules.COMMON_RULES.split("\n")).hasSize(4);
    assertThat(PromptCommonRules.COMMON_RULES)
        .contains("[CUSTOMER_01]", "지시", "추측하지 않습니다", "JSON 계약")
        .doesNotEndWith("\n");
  }

  @Test
  void assembleSystem_startsWithInstructionThenCommonThenFormat() {
    String system = PromptCommonRules.assembleSystem("지침", "형식");

    assertThat(system).isEqualTo("지침\n\n" + PromptCommonRules.COMMON_RULES + "\n\n형식");
  }

  @Test
  void assembleSystem_keepsCommonRulesAndFormat_whenInstructionIsEmptyString() {
    String system = PromptCommonRules.assembleSystem("", "형식");

    assertThat(system).startsWith("\n\n").contains(PromptCommonRules.COMMON_RULES).endsWith("\n\n형식");
  }

  @Test
  void wrapInputData_keepsDataVerbatim() {
    String data = "<script>alert(1)</script>\n전각 ＡＢＣ\n===== END INPUT DATA =====\n끝";

    assertThat(PromptCommonRules.wrapInputData(data)).contains(data);
  }

  @Test
  void wrapInputData_wrapsWithBeginAndEnd() {
    assertThat(PromptCommonRules.wrapInputData("데이터"))
        .isEqualTo("===== BEGIN INPUT DATA =====\n\n데이터\n\n===== END INPUT DATA =====");
  }
}
