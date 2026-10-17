package com.chuseok22.whoim.server.setting.core.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class SettingKeyTest {

  @Test
  void fromKey_returnsRegisteredKey() {
    assertThat(SettingKey.fromKey("llm.gemini.model")).contains(SettingKey.LLM_GEMINI_MODEL);
  }

  @Test
  void fromKey_returnsEmpty_whenUnknown() {
    assertThat(SettingKey.fromKey("unknown.key")).isEmpty();
  }

  @Test
  void validate_rejectsBlankString() {
    assertInvalid(SettingKey.LLM_GEMINI_MODEL, "  ");
    assertInvalid(SettingKey.LLM_GEMINI_MODEL, null);
  }

  @Test
  void validate_rejectsStringOverMaxLength() {
    assertInvalid(SettingKey.LLM_GEMINI_MODEL, "m".repeat(201));
  }

  @Test
  void validate_rejectsModelNameWithUnsafeCharacters() {
    assertInvalid(SettingKey.LLM_GEMINI_MODEL, "gemini flash");
    assertInvalid(SettingKey.LLM_GEMINI_MODEL, "gemini/../x");
    assertInvalid(SettingKey.LLM_SUH_AI_MODEL, "model?key=1");
    assertThatCode(() -> SettingKey.LLM_SUH_AI_MODEL.validate("gemma4:e4b")).doesNotThrowAnyException();
  }

  @Test
  void validate_rejectsNonNumericForDecimalAndInteger() {
    assertInvalid(SettingKey.LLM_GEMINI_TEMPERATURE, "abc");
    assertInvalid(SettingKey.LLM_GEMINI_TEMPERATURE, "NaN");
    assertInvalid(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS, "1.5");
    assertInvalid(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS, "abc");
  }

  @Test
  void validate_rejectsOutOfRange() {
    assertInvalid(SettingKey.LLM_GEMINI_TEMPERATURE, "2.1");
    assertInvalid(SettingKey.LLM_GEMINI_TEMPERATURE, "-0.1");
    assertInvalid(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS, "0");
    assertInvalid(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS, "1000001");
  }

  @Test
  void validate_acceptsValidValues() {
    assertThatCode(() -> {
      SettingKey.LLM_SUH_AI_TEMPERATURE.validate("0.0");
      SettingKey.LLM_SUH_AI_TEMPERATURE.validate("2.0");
      SettingKey.LLM_GEMINI_MODEL.validate("gemini-3.5-flash-lite");
      SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS.validate("20000");
    }).doesNotThrowAnyException();
  }

  private static void assertInvalid(SettingKey key, String value) {
    assertThatThrownBy(() -> key.validate(value))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTING_VALUE_INVALID));
  }
}
