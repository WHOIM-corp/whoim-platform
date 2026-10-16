package com.chuseok22.whoim.server.interview.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.interview.application.dto.data.ExtractedElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TraceExtractionValidatorTest {

  private final TraceExtractionValidator validator = new TraceExtractionValidator();

  private static ExtractedElement element(String name, String status, String content, String source, String excerpt) {
    return new ExtractedElement(name, status, content, source, excerpt);
  }

  @Test
  void accept_directExcerptMustBeInAnswer() {
    List<ExtractedElement> extracted = List.of(
        element("CAUSE", "FILLED", "재고 부족", "DIRECT", "재고가 부족했다"),
        element("RISK", "FILLED", "지연", "DIRECT", "답변에 없는 문장"));

    Map<TraceElement, TraceStateEntry> accepted =
        validator.accept(extracted, "그때는 재고가 부족했다", List.of(), 2);

    assertThat(accepted).containsOnlyKeys(TraceElement.CAUSE);
    assertThat(accepted.get(TraceElement.CAUSE).source()).isEqualTo(EvidenceLinkType.DIRECT);
    assertThat(accepted.get(TraceElement.CAUSE).turnNumber()).isEqualTo(2);
  }

  @Test
  void accept_toleratesWhitespaceAndNewlineDifferences() {
    List<ExtractedElement> extracted = List.of(
        element("CAUSE", "FILLED", "재고 부족", "DIRECT", "재고가 부족했다"));

    Map<TraceElement, TraceStateEntry> accepted =
        validator.accept(extracted, "재고가\n  부족했다", List.of(), 1);

    assertThat(accepted).containsOnlyKeys(TraceElement.CAUSE);
  }

  @Test
  void accept_recordedExcerptMustBeInAnchorExcerpts() {
    List<ExtractedElement> extracted = List.of(
        element("SIGNAL", "FILLED", "알림", "RECORDED", "경보가 울렸다"),
        element("CAUSE", "FILLED", "원인", "RECORDED", "없는 발췌"));

    Map<TraceElement, TraceStateEntry> accepted =
        validator.accept(extracted, "답변", List.of("앞 경보가 울렸다 뒤", "다른 발췌"), 1);

    assertThat(accepted).containsOnlyKeys(TraceElement.SIGNAL);
    assertThat(accepted.get(TraceElement.SIGNAL).source()).isEqualTo(EvidenceLinkType.RECORDED);
  }

  @Test
  void accept_dropsFilledWithBlankExcerpt() {
    List<ExtractedElement> extracted = List.of(
        element("CAUSE", "FILLED", "원인", "DIRECT", " "),
        element("RISK", "FILLED", "위험", "DIRECT", null),
        element("ACTION", "FILLED", " ", "DIRECT", "답변"));

    assertThat(validator.accept(extracted, "답변", List.of(), 1)).isEmpty();
  }

  @Test
  void accept_dropsUnknownElementName() {
    List<ExtractedElement> extracted = List.of(
        element("MOOD", "UNKNOWN", null, null, null),
        element("cause", "UNKNOWN", null, null, null));

    assertThat(validator.accept(extracted, "답변", List.of(), 1)).isEmpty();
  }

  @Test
  void accept_dropsLowercaseStatus() {
    List<ExtractedElement> extracted = List.of(
        element("CAUSE", "filled", "원인", "DIRECT", "답변"),
        element("RISK", "unknown", null, null, null),
        element("ACTION", "FILLED", "행동", "direct", "답변"));

    assertThat(validator.accept(extracted, "답변", List.of(), 1)).isEmpty();
  }

  @Test
  void accept_keepsUnknownStatus() {
    List<ExtractedElement> extracted = List.of(
        element("CAUSE", "UNKNOWN", "무시될 내용", "DIRECT", "무시"));

    Map<TraceElement, TraceStateEntry> accepted = validator.accept(extracted, "모르겠다", List.of(), 3);

    TraceStateEntry entry = accepted.get(TraceElement.CAUSE);
    assertThat(entry.status()).isEqualTo(TraceElementStatus.UNKNOWN);
    assertThat(entry.content()).isNull();
    assertThat(entry.source()).isNull();
    assertThat(entry.turnNumber()).isEqualTo(3);
  }

  @Test
  void accept_survivesPartiallyInvalidList() {
    List<ExtractedElement> extracted = List.of(
        element("MOOD", "FILLED", "x", "DIRECT", "답변"),
        element("CAUSE", "FILLED", "원인", "DIRECT", "답변"),
        element("RISK", "UNKNOWN", null, null, null),
        element("ACTION", "FILLED", "행동", "DIRECT", ""));

    Map<TraceElement, TraceStateEntry> accepted = validator.accept(extracted, "답변", List.of(), 1);

    assertThat(accepted).containsOnlyKeys(TraceElement.CAUSE, TraceElement.RISK);
  }

  @Test
  void accept_dropsBlankElementAndNullStatus() {
    List<ExtractedElement> extracted = List.of(
        element(" ", "UNKNOWN", null, null, null),
        element(null, "UNKNOWN", null, null, null),
        element("CAUSE", null, "원인", "DIRECT", "답변"),
        element("RISK", "UNKNOWN", null, null, null));

    assertThat(validator.accept(extracted, "답변", List.of(), 1)).containsOnlyKeys(TraceElement.RISK);
  }

  @Test
  void accept_skipsNullListElement() {
    List<ExtractedElement> extracted = new java.util.ArrayList<>();
    extracted.add(null);
    extracted.add(element("RISK", "UNKNOWN", null, null, null));

    assertThat(validator.accept(extracted, "답변", List.of(), 1)).containsOnlyKeys(TraceElement.RISK);
  }

  @Test
  void accept_lastAcceptedDuplicateWins() {
    List<ExtractedElement> extracted = List.of(
        element("CAUSE", "FILLED", "첫째", "DIRECT", "답변"),
        element("CAUSE", "FILLED", "둘째", "DIRECT", "답변"),
        element("CAUSE", "FILLED", "무효", "DIRECT", "없음"));

    assertThat(validator.accept(extracted, "답변", List.of(), 1).get(TraceElement.CAUSE).content())
        .isEqualTo("둘째");
  }
}
