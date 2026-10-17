package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DlpMaskingSessionTest {

  @Test
  void tokenFor_sameValueSameTokenDifferentValueDifferentToken() {
    DlpMaskingSession session = new DlpMaskingSession();

    String first = session.tokenFor("가나다상사", DlpEntityType.CUSTOMER);
    String again = session.tokenFor("가나다상사", DlpEntityType.CUSTOMER);
    String other = session.tokenFor("라마바상사", DlpEntityType.CUSTOMER);

    assertThat(first).isEqualTo("[CUSTOMER_01]").isEqualTo(again);
    assertThat(other).isEqualTo("[CUSTOMER_02]");
  }

  @Test
  void tokenFor_comparesCaseInsensitively() {
    DlpMaskingSession session = new DlpMaskingSession();

    assertThat(session.tokenFor("Project Alpha", DlpEntityType.PROJECT))
        .isEqualTo(session.tokenFor("project alpha", DlpEntityType.PROJECT));
  }

  @Test
  void restore_restoresPseudonymTokens() {
    DlpMaskingSession session = new DlpMaskingSession();
    String token = session.tokenFor("김가나", DlpEntityType.PERSON);

    assertThat(session.restore("담당은 " + token + "입니다")).isEqualTo("담당은 김가나입니다");
  }

  @Test
  void restore_restoresRedactedTokens() {
    DlpMaskingSession session = new DlpMaskingSession();
    String token = session.tokenFor("user@example.com", DlpEntityType.EMAIL);

    assertThat(token).isEqualTo("[REDACTED_EMAIL_01]");
    assertThat(session.restore(token)).isEqualTo("user@example.com");
  }

  @Test
  void redactedTokens_areNumberedPerValue() {
    DlpMaskingSession session = new DlpMaskingSession();

    assertThat(session.tokenFor("a@example.com", DlpEntityType.EMAIL)).isEqualTo("[REDACTED_EMAIL_01]");
    assertThat(session.tokenFor("b@example.com", DlpEntityType.EMAIL)).isEqualTo("[REDACTED_EMAIL_02]");
    assertThat(session.tokenFor("10.0.0.1", DlpEntityType.IP_ADDRESS)).isEqualTo("[REDACTED_IP_01]");
  }

  @Test
  void restore_longerTokenFirst() {
    DlpMaskingSession session = new DlpMaskingSession();
    for (int index = 1; index <= 10; index++) {
      session.tokenFor("이름" + index, DlpEntityType.PERSON);
    }

    assertThat(session.restore("[PERSON_10] [PERSON_01]")).isEqualTo("이름10 이름1");
  }

  @Test
  void restore_escapesJsonSpecialCharacters() throws Exception {
    DlpMaskingSession session = new DlpMaskingSession();
    String token = session.tokenFor("그는 \"안녕\"\\n\n이라 했다", DlpEntityType.PERSON);
    String content = "{\"answer\":\"" + token + "\"}";

    String restored = session.restore(content);

    assertThat(JsonMapper.builder().build().readTree(restored).get("answer").asString())
        .isEqualTo("그는 \"안녕\"\\n\n이라 했다");
  }

  @Test
  void restore_leavesUnknownTokens() {
    DlpMaskingSession session = new DlpMaskingSession();
    session.tokenFor("김가나", DlpEntityType.PERSON);

    assertThat(session.restore("[PERSON_09] [UNKNOWN_01]")).isEqualTo("[PERSON_09] [UNKNOWN_01]");
  }

  @Test
  void counts_countsDistinctValuesPerType() {
    DlpMaskingSession session = new DlpMaskingSession();
    session.tokenFor("a@example.com", DlpEntityType.EMAIL);
    session.tokenFor("a@example.com", DlpEntityType.EMAIL);
    session.tokenFor("b@example.com", DlpEntityType.EMAIL);

    assertThat(session.counts()).containsEntry(DlpEntityType.EMAIL, 2).hasSize(1);
  }

  @Test
  void toString_hasNoOriginal() {
    DlpMaskingSession session = new DlpMaskingSession();
    session.tokenFor("비밀원문", DlpEntityType.PERSON);

    assertThat(session.toString()).doesNotContain("비밀원문");
  }
}
