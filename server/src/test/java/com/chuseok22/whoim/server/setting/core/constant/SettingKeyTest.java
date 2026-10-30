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
  void fromKey_returnsConsultationPromptKeys() {
    assertThat(SettingKey.fromKey("prompt.consultation-questions")).contains(SettingKey.PROMPT_CONSULTATION_QUESTIONS);
    assertThat(SettingKey.fromKey("prompt.consultation-suggestions"))
        .contains(SettingKey.PROMPT_CONSULTATION_SUGGESTIONS);
  }

  @Test
  void allPromptDefaults_passValidation() {
    for (SettingKey key : SettingKey.values()) {
      if (key.key().startsWith("prompt.")) {
        assertThatCode(() -> key.validate(key.defaultValue())).doesNotThrowAnyException();
      }
    }
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

  @Test
  void validate_acceptsMultiLineTermsIgnoringBlankLines() {
    assertThatCode(() -> SettingKey.DLP_ENTITIES_CUSTOMER.validate("가나다상사\n\n  라마바물류  \r\n\n")).doesNotThrowAnyException();
    assertThatCode(() -> SettingKey.DLP_ENTITIES_CUSTOMER.validate("가" .repeat(2) + "\n" + "a".repeat(100)))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_rejectsLinesTermOutsideLengthRange() {
    assertInvalid(SettingKey.DLP_ENTITIES_PERSON, "가나\n가");
    assertInvalid(SettingKey.DLP_ENTITIES_PERSON, "a".repeat(101));
  }

  @Test
  void validate_rejectsTooManyLinesTerms() {
    String fiveHundred = java.util.stream.IntStream.range(0, 500).mapToObj(i -> "term" + i)
        .collect(java.util.stream.Collectors.joining("\n"));
    assertThatCode(() -> SettingKey.DLP_ENTITIES_PROJECT.validate(fiveHundred)).doesNotThrowAnyException();
    assertInvalid(SettingKey.DLP_ENTITIES_PROJECT, fiveHundred + "\nterm500");
  }

  @Test
  void validate_rejectsLinesValueOverTotalLength() {
    String overLimit = java.util.stream.IntStream.range(0, 400).mapToObj(i -> "a".repeat(20))
        .collect(java.util.stream.Collectors.joining("\n"));
    assertInvalid(SettingKey.DLP_ENTITIES_PARTNER, overLimit);
  }

  @Test
  void validate_doesNotLeakLinesValueInMessage() {
    String secretTerm = "비공개고객사" .repeat(30);
    assertThatThrownBy(() -> SettingKey.DLP_ENTITIES_CUSTOMER.validate(secretTerm))
        .isInstanceOf(CustomException.class)
        .satisfies(exception -> assertThat(String.valueOf(exception.getMessage())).doesNotContain("비공개고객사"));
  }

  @Test
  void hintGetters_exposeRangeAndMaxLength() {
    assertThat(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS.minimum()).isEqualTo(1);
    assertThat(SettingKey.GAP_ANALYSIS_MAX_INPUT_CHARACTERS.maximum()).isEqualTo(1_000_000);
    assertThat(SettingKey.PROMPT_GAP_ANALYSIS.maxLength()).isEqualTo(8000);
  }

  private static void assertInvalid(SettingKey key, String value) {
    assertThatThrownBy(() -> key.validate(value))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTING_VALUE_INVALID));
  }
}
