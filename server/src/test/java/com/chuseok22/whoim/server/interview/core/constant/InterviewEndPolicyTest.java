package com.chuseok22.whoim.server.interview.core.constant;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InterviewEndPolicyTest {

  private static TraceState allFilled() {
    Map<TraceElement, TraceStateEntry> entries = new EnumMap<>(TraceElement.class);
    for (TraceElement element : TraceElement.values()) {
      entries.put(element, new TraceStateEntry(TraceElementStatus.FILLED, "내용", EvidenceLinkType.DIRECT, 1));
    }
    return TraceState.empty().merge(entries);
  }

  @Test
  void decide_checklistCompleteBeatsTurnLimit() {
    assertThat(InterviewEndPolicy.decide(allFilled(), 5)).contains(InterviewEndReason.CHECKLIST_COMPLETE);
  }

  @Test
  void decide_turnLimitAtFifthAnswer() {
    assertThat(InterviewEndPolicy.decide(TraceState.empty(), 5)).contains(InterviewEndReason.TURN_LIMIT);
  }

  @Test
  void decide_continuesBeforeFifthWhenIncomplete() {
    assertThat(InterviewEndPolicy.decide(TraceState.empty(), 4)).isEmpty();
  }

  @Test
  void decide_fiveUnknownAnswersEndWithTurnLimit() {
    TraceState state = TraceState.empty();
    for (int turn = 1; turn <= 5; turn++) {
      state = state.merge(Map.of(TraceElement.CAUSE,
          new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, turn)));
    }

    assertThat(InterviewEndPolicy.decide(state, 5)).contains(InterviewEndReason.TURN_LIMIT);
  }
}
