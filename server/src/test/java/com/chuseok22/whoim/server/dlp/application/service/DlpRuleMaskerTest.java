package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DlpRuleMaskerTest {

  private final DlpRuleMasker masker = maskerWith(Map.of(
      SettingKey.DLP_ENTITIES_CUSTOMER, List.of("가나다상사"),
      SettingKey.DLP_ENTITIES_PARTNER, List.of("라마바물류"),
      SettingKey.DLP_ENTITIES_PROJECT, List.of("Project Alpha"),
      SettingKey.DLP_ENTITIES_PERSON, List.of("김가나")));

  private static DlpRuleMasker maskerWith(Map<SettingKey, List<String>> termsByKey) {
    SettingConfigService settingConfigService = mock(SettingConfigService.class);
    for (SettingKey key : SettingKey.values()) {
      when(settingConfigService.getLines(key)).thenReturn(termsByKey.getOrDefault(key, List.of()));
    }
    return new DlpRuleMasker(settingConfigService);
  }

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
    DlpRuleMasker shortAndLong = maskerWith(Map.of(SettingKey.DLP_ENTITIES_CUSTOMER, List.of("A사", "A사업부")));

    String masked = shortAndLong.maskByRules("A사업부와 A사", new DlpMaskingSession());

    assertThat(masked).isEqualTo("[CUSTOMER_01]와 [CUSTOMER_02]");
  }

  @Test
  void maskByRules_appliesDictionaryChangeOnNextCall() {
    SettingConfigService settingConfigService = mock(SettingConfigService.class);
    for (SettingKey key : SettingKey.values()) {
      when(settingConfigService.getLines(key)).thenReturn(List.of());
    }
    DlpRuleMasker liveMasker = new DlpRuleMasker(settingConfigService);

    assertThat(liveMasker.maskByRules("가나다상사 방문", new DlpMaskingSession())).isEqualTo("가나다상사 방문");

    when(settingConfigService.getLines(SettingKey.DLP_ENTITIES_CUSTOMER)).thenReturn(List.of("가나다상사"));

    assertThat(liveMasker.maskByRules("가나다상사 방문", new DlpMaskingSession())).isEqualTo("[CUSTOMER_01] 방문");
  }

  @Test
  void maskByRules_masksNothingByDictionary_whenAllListsEmpty() {
    DlpRuleMasker emptyMasker = maskerWith(Map.of());

    assertThat(emptyMasker.maskByRules("가나다상사와 김가나", new DlpMaskingSession())).isEqualTo("가나다상사와 김가나");
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
    DlpRuleMasker blankTerms = maskerWith(Map.of(SettingKey.DLP_ENTITIES_CUSTOMER, List.of("", "  ")));

    assertThat(blankTerms.maskByRules("아무 말이나", new DlpMaskingSession())).isEqualTo("아무 말이나");
  }

  @Test
  void maskByRules_doesNotRemaskTokens() {
    DlpRuleMasker tokenLikeTerms = maskerWith(Map.of(SettingKey.DLP_ENTITIES_CUSTOMER, List.of("CUSTOMER", "EMAIL")));
    String text = "[CUSTOMER_01] 와 [REDACTED_EMAIL_01]";

    assertThat(tokenLikeTerms.maskByRules(text, new DlpMaskingSession())).isEqualTo(text);
  }

  @Test
  void maskByRules_masksDictionaryTermInsideBrackets() {
    DlpRuleMasker bracketTerm = maskerWith(Map.of(SettingKey.DLP_ENTITIES_PROJECT, List.of("PHOENIX")));

    assertThat(bracketTerm.maskByRules("[PHOENIX] 배포 일정", new DlpMaskingSession()))
        .isEqualTo("[[PROJECT_01]] 배포 일정");
  }

  @Test
  void maskByRules_fallsBackToOther_whenDictionaryLookupMisses() {
    DlpRuleMasker unicodeFold = maskerWith(Map.of(SettingKey.DLP_ENTITIES_CUSTOMER, List.of("s")));

    assertThat(unicodeFold.maskByRules("ſ", new DlpMaskingSession())).isEqualTo("[SENSITIVE_01]");
  }

  @Test
  void maskByRules_masksEnAndMinusHyphenInPhoneAndRrn() {
    String masked = masker.maskByRules("전화 010–1234–5678, 주민 900101−1234567", new DlpMaskingSession());

    assertThat(masked).isEqualTo("전화 [REDACTED_PHONE_01], 주민 [REDACTED_RRN_01]");
  }
}
