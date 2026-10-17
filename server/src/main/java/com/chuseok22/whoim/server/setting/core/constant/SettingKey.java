package com.chuseok22.whoim.server.setting.core.constant;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

// 설정 키의 단일 목록. 새 설정은 여기에 추가한다
public enum SettingKey {

  // 기본값 null은 서버 설정(application-*.yml) 값을 쓴다는 뜻이다
  LLM_SUH_AI_MODEL("llm.suh-ai.model", "SUH AI 모델명", SettingValueType.STRING, null, 0, 0, 200),
  LLM_SUH_AI_TEMPERATURE("llm.suh-ai.temperature", "SUH AI temperature", SettingValueType.DECIMAL, "0.0", 0.0, 2.0, 0),
  LLM_GEMINI_MODEL("llm.gemini.model", "Gemini 모델명", SettingValueType.STRING, null, 0, 0, 200),
  LLM_GEMINI_TEMPERATURE("llm.gemini.temperature", "Gemini temperature", SettingValueType.DECIMAL, "0.2", 0.0, 2.0, 0),
  GAP_ANALYSIS_MAX_INPUT_CHARACTERS("gap.analysis.max-input-characters", "Gap 분석 입력 최대 글자 수",
      SettingValueType.INTEGER, "20000", 1, 1_000_000, 0),
  PROMPT_GAP_ANALYSIS("prompt.gap-analysis", "Gap 분석 프롬프트 지침", SettingValueType.STRING,
      DefaultPromptInstructions.GAP_ANALYSIS, 0, 0, 8000),
  PROMPT_INTERVIEW_FIRST_QUESTION("prompt.interview-first-question", "인터뷰 첫 질문 프롬프트 지침",
      SettingValueType.STRING, DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION, 0, 0, 8000),
  PROMPT_INTERVIEW_TURN("prompt.interview-turn", "인터뷰 턴 프롬프트 지침", SettingValueType.STRING,
      DefaultPromptInstructions.INTERVIEW_TURN, 0, 0, 8000),
  PROMPT_CARD_GENERATION("prompt.card-generation", "Card 생성 프롬프트 지침", SettingValueType.STRING,
      DefaultPromptInstructions.CARD_GENERATION, 0, 0, 8000),
  PROMPT_DLP_DETECTION("prompt.dlp-detection", "DLP 탐지 프롬프트 지침", SettingValueType.STRING,
      DefaultPromptInstructions.DLP_DETECTION, 0, 0, 8000);

  // SUH AI 모델명은 name:tag 형태라 콜론을 허용한다
  private static final Pattern MODEL_NAME = Pattern.compile("^[A-Za-z0-9._:-]+$");

  private final String key;
  private final String description;
  private final SettingValueType valueType;
  private final String defaultValue;
  private final double minimum;
  private final double maximum;
  private final int maxLength;

  SettingKey(String key, String description, SettingValueType valueType, String defaultValue,
      double minimum, double maximum, int maxLength) {
    this.key = key;
    this.description = description;
    this.valueType = valueType;
    this.defaultValue = defaultValue;
    this.minimum = minimum;
    this.maximum = maximum;
    this.maxLength = maxLength;
  }

  public String key() {
    return key;
  }

  public String description() {
    return description;
  }

  public SettingValueType valueType() {
    return valueType;
  }

  public String defaultValue() {
    return defaultValue;
  }

  public static Optional<SettingKey> fromKey(String key) {
    return Arrays.stream(values())
        .filter(settingKey -> settingKey.key.equals(key))
        .findFirst();
  }

  // 위반 시 값을 메시지에 담지 않고 SETTING_VALUE_INVALID만 던진다
  public void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.SETTING_VALUE_INVALID);
    }
    switch (valueType) {
      case STRING -> validateString(value);
      case INTEGER -> validateNumber(parseInteger(value));
      case DECIMAL -> validateNumber(parseDecimal(value));
    }
  }

  private void validateString(String value) {
    boolean modelNameKey = this == LLM_SUH_AI_MODEL || this == LLM_GEMINI_MODEL;
    if (value.length() > maxLength || (modelNameKey && !MODEL_NAME.matcher(value).matches())) {
      throw new CustomException(ErrorCode.SETTING_VALUE_INVALID);
    }
  }

  private void validateNumber(double number) {
    if (!Double.isFinite(number) || number < minimum || number > maximum) {
      throw new CustomException(ErrorCode.SETTING_VALUE_INVALID);
    }
  }

  private static double parseInteger(String value) {
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException exception) {
      throw new CustomException(ErrorCode.SETTING_VALUE_INVALID);
    }
  }

  private static double parseDecimal(String value) {
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException exception) {
      throw new CustomException(ErrorCode.SETTING_VALUE_INVALID);
    }
  }
}
