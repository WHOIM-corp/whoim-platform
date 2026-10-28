package com.chuseok22.whoim.server.card.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvidenceElementResolverTest {

  private final EvidenceElementResolver resolver = new EvidenceElementResolver();

  @Test
  void direct_returnsElementsFilledByThatTurn() {
    TraceState state = new TraceState(Map.of(
        TraceElement.ALTERNATIVE, filled(EvidenceLinkType.DIRECT, 2),
        TraceElement.JUDGMENT, filled(EvidenceLinkType.DIRECT, 2),
        TraceElement.CAUSE, filled(EvidenceLinkType.DIRECT, 1)));

    assertThat(resolver.resolve(state, directLinkAt(2)))
        .containsExactly(TraceElement.JUDGMENT, TraceElement.ALTERNATIVE);
  }

  @Test
  void direct_ignoresUnknownAndRecordedSourceEntries() {
    TraceState state = new TraceState(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 2),
        TraceElement.ACTION, filled(EvidenceLinkType.RECORDED, 2),
        TraceElement.JUDGMENT, filled(EvidenceLinkType.DIRECT, 2)));

    assertThat(resolver.resolve(state, directLinkAt(2))).containsExactly(TraceElement.JUDGMENT);
  }

  @Test
  void direct_turnWithoutFilledElement_returnsEmpty() {
    TraceState state = new TraceState(Map.of(TraceElement.CAUSE, filled(EvidenceLinkType.DIRECT, 1)));

    assertThat(resolver.resolve(state, directLinkAt(3))).isEmpty();
  }

  @Test
  void recorded_returnsEmpty() {
    TraceState state = new TraceState(Map.of(TraceElement.CAUSE, filled(EvidenceLinkType.DIRECT, 1)));
    CardEvidenceLink recorded = CardEvidenceLink.builder()
        .type(EvidenceLinkType.RECORDED).excerpt("본문").build();

    assertThat(resolver.resolve(state, recorded)).isEmpty();
  }

  private static TraceStateEntry filled(EvidenceLinkType source, int turnNumber) {
    return new TraceStateEntry(TraceElementStatus.FILLED, "내용", source, turnNumber);
  }

  private static CardEvidenceLink directLinkAt(int turnNumber) {
    InterviewTurn turn = InterviewTurn.builder().turnNumber(turnNumber).question("질문").build();
    return CardEvidenceLink.builder().type(EvidenceLinkType.DIRECT).turn(turn).excerpt("발언").build();
  }
}
