package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.dlp.infrastructure.properties.DlpProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DlpRuleMaskerTest {

  private final DlpRuleMasker masker = new DlpRuleMasker(new DlpProperties(Map.of(
      DlpEntityType.CUSTOMER, List.of("가나다상사"),
      DlpEntityType.PARTNER, List.of("라마바물류"),
      DlpEntityType.PROJECT, List.of("Project Alpha"),
      DlpEntityType.PERSON, List.of("김가나"))));

  @Test
  void maskByRules_redactsIpEmailPhoneRrnAndKey() {
    String text = "서버 10.20.30.40, 메일 user@example.com, 전화 010-1234-5678, "
        + "주민 900101-1234567, 키 sk-abcdefghijklmnopqrstuvwx";

    String masked = masker.maskByRules(text, new DlpMaskingSession());

    assertThat(masked)
        .contains("[REDACTED_IP_01]", "[REDACTED_EMAIL_01]", "[REDACTED_PHONE_01]",
            "[REDACTED_RRN_01]", "[REDACTED_API_KEY_01]")
        .doesNotContain("10.20.30.40", "user@example.com", "010-1234-5678", "900101-1234567", "sk-abc");
  }

  @Test
  void maskByRules_roundTripsThroughRestore() {
    DlpMaskingSession session = new DlpMaskingSession();
    String text = "연락처 010-1234-5678 와 user@example.com";

    assertThat(session.restore(masker.maskByRules(text, session))).isEqualTo(text);
  }

  @Test
  void maskByRules_pseudonymizesDictionaryEntities() {
    String masked = masker.maskByRules("가나다상사와 가나다상사, 그리고 라마바물류", new DlpMaskingSession());

    assertThat(masked).isEqualTo("[CUSTOMER_01]와 [CUSTOMER_01], 그리고 [PARTNER_01]");
  }

  @Test
  void maskByRules_treatsCaseVariantsAsSameEntity() {
    String masked = masker.maskByRules("project alpha 와 Project Alpha", new DlpMaskingSession());

    assertThat(masked).isEqualTo("[PROJECT_01] 와 [PROJECT_01]");
  }

  @Test
  void maskByRules_prefersLongerDictionaryTerm() {
    DlpRuleMasker shortAndLong = new DlpRuleMasker(new DlpProperties(Map.of(
        DlpEntityType.CUSTOMER, List.of("A사", "A사업부"))));

    String masked = shortAndLong.maskByRules("A사업부와 A사", new DlpMaskingSession());

    assertThat(masked).isEqualTo("[CUSTOMER_01]와 [CUSTOMER_02]");
  }

  @Test
  void maskByRules_masksFullWidthVariants() {
    String masked = masker.maskByRules("ｔｅｓｔ＠ｅｘａｍｐｌｅ．ｃｏｍ 와 ０１０－１２３４－５６７８", new DlpMaskingSession());

    assertThat(masked).isEqualTo("[REDACTED_EMAIL_01] 와 [REDACTED_PHONE_01]");
  }

  @Test
  void maskByRules_keepsNormalNumbers() {
    String text = "999.1.1.1 / 2026-10-17 / 버전 1.2.3 / 123456789";

    assertThat(masker.maskByRules(text, new DlpMaskingSession())).isEqualTo(text);
  }

  @Test
  void maskByRules_ignoresBlankDictionaryTerms() {
    DlpRuleMasker blankTerms = new DlpRuleMasker(new DlpProperties(Map.of(
        DlpEntityType.CUSTOMER, List.of("", "  "))));

    assertThat(blankTerms.maskByRules("아무 말이나", new DlpMaskingSession())).isEqualTo("아무 말이나");
  }

  @Test
  void maskByRules_doesNotRemaskTokens() {
    DlpRuleMasker tokenLikeTerms = new DlpRuleMasker(new DlpProperties(Map.of(
        DlpEntityType.CUSTOMER, List.of("CUSTOMER", "EMAIL"))));
    String text = "[CUSTOMER_01] 와 [REDACTED_EMAIL_01]";

    assertThat(tokenLikeTerms.maskByRules(text, new DlpMaskingSession())).isEqualTo(text);
  }

  @Test
  void maskByRules_masksDictionaryTermInsideBrackets() {
    DlpRuleMasker bracketTerm = new DlpRuleMasker(new DlpProperties(Map.of(
        DlpEntityType.PROJECT, List.of("PHOENIX"))));

    assertThat(bracketTerm.maskByRules("[PHOENIX] 배포 일정", new DlpMaskingSession()))
        .isEqualTo("[[PROJECT_01]] 배포 일정");
  }

  @Test
  void maskByRules_fallsBackToOther_whenDictionaryLookupMisses() {
    DlpRuleMasker unicodeFold = new DlpRuleMasker(new DlpProperties(Map.of(
        DlpEntityType.CUSTOMER, List.of("s"))));

    assertThat(unicodeFold.maskByRules("ſ", new DlpMaskingSession())).isEqualTo("[SENSITIVE_01]");
  }

  @Test
  void maskByRules_masksEnAndMinusHyphenInPhoneAndRrn() {
    String masked = masker.maskByRules("전화 010–1234–5678, 주민 900101−1234567", new DlpMaskingSession());

    assertThat(masked).isEqualTo("전화 [REDACTED_PHONE_01], 주민 [REDACTED_RRN_01]");
  }
}
