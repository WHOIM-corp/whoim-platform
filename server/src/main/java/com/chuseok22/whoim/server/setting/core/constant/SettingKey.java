package com.chuseok22.whoim.server.setting.core.constant;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
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
  PROMPT_CONSULTATION_QUESTIONS("prompt.consultation-questions", "상담 질문 생성 프롬프트 지침",
      SettingValueType.STRING, DefaultPromptInstructions.CONSULTATION_QUESTIONS, 0, 0, 8000),
  PROMPT_CONSULTATION_SUGGESTIONS("prompt.consultation-suggestions", "상담 Card 제시 프롬프트 지침",
      SettingValueType.STRING, DefaultPromptInstructions.CONSULTATION_SUGGESTIONS, 0, 0, 8000),
  PROMPT_DLP_DETECTION("prompt.dlp-detection", "DLP 탐지 프롬프트 지침", SettingValueType.STRING,
      DefaultPromptInstructions.DLP_DETECTION, 0, 0, 8000),
  DLP_ENTITIES_CUSTOMER("dlp.entities.customer", "DLP 사전: 고객사 용어(줄바꿈으로 구분)", SettingValueType.LINES, "", 0, 0,
      8000),
  DLP_ENTITIES_PARTNER("dlp.entities.partner", "DLP 사전: 협력사 용어(줄바꿈으로 구분)", SettingValueType.LINES, "", 0, 0,
      8000),
  DLP_ENTITIES_PROJECT("dlp.entities.project", "DLP 사전: 프로젝트 용어(줄바꿈으로 구분)", SettingValueType.LINES, "", 0, 0,
      8000),
  DLP_ENTITIES_PERSON("dlp.entities.person", "DLP 사전: 인명 용어(줄바꿈으로 구분)", SettingValueType.LINES, "", 0, 0,
      8000);

  // SUH AI 모델명은 name:tag 형태라 콜론을 허용한다
  private static final Pattern MODEL_NAME = Pattern.compile("^[A-Za-z0-9._:-]+$");

  private static final int MIN_TERM_LENGTH = 2;
  private static final int MAX_TERM_LENGTH = 100;
  private static final int MAX_TERM_COUNT = 500;

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

  public double minimum() {
    return minimum;
  }

  public double maximum() {
    return maximum;
  }

  public int maxLength() {
    return maxLength;
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
      case LINES -> validateLines(value);
    }
  }

  private void validateString(String value) {
    boolean modelNameKey = this == LLM_SUH_AI_MODEL || this == LLM_GEMINI_MODEL;
    if (value.length() > maxLength || (modelNameKey && !MODEL_NAME.matcher(value).matches())) {
      throw new CustomException(ErrorCode.SETTING_VALUE_INVALID);
    }
  }

  // 빈 줄은 무시하고 용어는 앞뒤 공백을 제거한 길이로 검사한다
  private void validateLines(String value) {
    List<String> terms = value.lines().map(String::trim).filter(term -> !term.isEmpty()).toList();
    boolean invalidTerm = terms.stream()
        .anyMatch(term -> term.length() < MIN_TERM_LENGTH || term.length() > MAX_TERM_LENGTH);
    if (value.length() > maxLength || terms.size() > MAX_TERM_COUNT || invalidTerm) {
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
