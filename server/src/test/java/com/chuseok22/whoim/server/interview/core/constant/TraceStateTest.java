package com.chuseok22.whoim.server.interview.core.constant;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TraceStateTest {

  private static TraceStateEntry filled(String content, int turn) {
    return new TraceStateEntry(TraceElementStatus.FILLED, content, EvidenceLinkType.DIRECT, turn);
  }

  private static TraceStateEntry unknown(int turn) {
    return new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, turn);
  }

  private static TraceState allFilled() {
    Map<TraceElement, TraceStateEntry> entries = new EnumMap<>(TraceElement.class);
    for (TraceElement element : TraceElement.values()) {
      entries.put(element, filled("내용", 1));
    }
    return TraceState.empty().merge(entries);
  }

  @Test
  void merge_filledIsNotOverwrittenByUnknown() {
    TraceState state = TraceState.empty().merge(Map.of(TraceElement.CAUSE, filled("원인", 1)));

    TraceState merged = state.merge(Map.of(TraceElement.CAUSE, unknown(2)));

    assertThat(merged.entries().get(TraceElement.CAUSE).content()).isEqualTo("원인");
    assertThat(merged.entries().get(TraceElement.CAUSE).status()).isEqualTo(TraceElementStatus.FILLED);
  }

  @Test
  void merge_unknownIsReplacedByFilled() {
    TraceState state = TraceState.empty().merge(Map.of(TraceElement.CAUSE, unknown(1)));

    TraceState merged = state.merge(Map.of(TraceElement.CAUSE, filled("원인", 2)));

    assertThat(merged.entries().get(TraceElement.CAUSE).status()).isEqualTo(TraceElementStatus.FILLED);
    assertThat(merged.entries().get(TraceElement.CAUSE).turnNumber()).isEqualTo(2);
  }

  @Test
  void merge_newerFilledWins() {
    TraceState state = TraceState.empty().merge(Map.of(TraceElement.CAUSE, filled("옛 원인", 1)));

    TraceState merged = state.merge(Map.of(TraceElement.CAUSE, filled("새 원인", 3)));

    assertThat(merged.entries().get(TraceElement.CAUSE).content()).isEqualTo("새 원인");
  }

  @Test
  void merge_doesNotMutateOriginal() {
    TraceState state = TraceState.empty();

    TraceState merged = state.merge(Map.of(TraceElement.RISK, filled("위험", 1)));

    assertThat(state.entries()).isEmpty();
    assertThat(merged.entries()).hasSize(1);
  }

  @Test
  void isChecklistComplete_requiresAllTenFilled() {
    TraceState complete = allFilled();
    assertThat(complete.isChecklistComplete()).isTrue();

    TraceState withUnknown = complete.merge(Map.of(TraceElement.RISK, unknown(2)));
    assertThat(withUnknown.isChecklistComplete()).isTrue();

    Map<TraceElement, TraceStateEntry> entries = new EnumMap<>(TraceElement.class);
    for (TraceElement element : TraceElement.values()) {
      entries.put(element, element == TraceElement.RISK ? unknown(1) : filled("내용", 1));
    }
    assertThat(TraceState.empty().merge(entries).isChecklistComplete()).isFalse();
  }

  @Test
  void hasDirectFilledElement_requiresFilledAndDirect() {
    TraceStateEntry recorded = new TraceStateEntry(TraceElementStatus.FILLED, "x", EvidenceLinkType.RECORDED, 1);
    TraceState state = TraceState.empty().merge(Map.of(
        TraceElement.CAUSE, recorded, TraceElement.RISK, unknown(1)));
    assertThat(state.hasDirectFilledElement()).isFalse();

    assertThat(state.merge(Map.of(TraceElement.ACTION, filled("행동", 2))).hasDirectFilledElement()).isTrue();
  }

  @Test
  void missingElements_inDeclarationOrder() {
    TraceState state = TraceState.empty().merge(Map.of(
        TraceElement.SIGNAL, filled("신호", 1),
        TraceElement.ACTION, unknown(1)));

    List<TraceElement> missing = state.missingElements();

    assertThat(missing).containsExactly(
        TraceElement.SITUATION, TraceElement.CAUSE, TraceElement.CONDITION, TraceElement.JUDGMENT,
        TraceElement.ALTERNATIVE, TraceElement.ACTION, TraceElement.REASON, TraceElement.EXCEPTION,
        TraceElement.RISK);
    assertThat(state.hasFilledElement()).isTrue();
    assertThat(TraceState.empty().hasFilledElement()).isFalse();
  }
}
