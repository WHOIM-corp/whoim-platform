package com.chuseok22.whoim.server.dlp.core.constant;

import java.util.Locale;

public enum DlpEntityType {
  IP_ADDRESS("IP", false),
  EMAIL("EMAIL", false),
  PHONE_NUMBER("PHONE", false),
  RESIDENT_REGISTRATION_NUMBER("RRN", false),
  API_KEY("API_KEY", false),
  CUSTOMER("CUSTOMER", true),
  PARTNER("PARTNER", true),
  PROJECT("PROJECT", true),
  PERSON("PERSON", true),
  SECRET("SECRET", false),
  OTHER("SENSITIVE", true);

  private final String label;
  private final boolean pseudonymized;

  DlpEntityType(String label, boolean pseudonymized) {
    this.label = label;
    this.pseudonymized = pseudonymized;
  }

  // 가명 처리 유형은 문맥을 살리려고 [LABEL_01], 그 외는 [REDACTED_LABEL_01] 토큰을 쓴다
  public boolean isPseudonymized() {
    return pseudonymized;
  }

  public String label() {
    return label;
  }

  // AI는 5개 라벨만 허용하고 그 밖의 값은 OTHER로 흡수한다
  public static DlpEntityType fromAiLabel(String aiLabel) {
    if (aiLabel == null) {
      return OTHER;
    }
    return switch (aiLabel.trim().toUpperCase(Locale.ROOT)) {
      case "CUSTOMER" -> CUSTOMER;
      case "PARTNER" -> PARTNER;
      case "PROJECT" -> PROJECT;
      case "PERSON" -> PERSON;
      case "SECRET" -> SECRET;
      default -> OTHER;
    };
  }
}
