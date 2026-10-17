package com.chuseok22.whoim.server.interview.core.constant;

import java.util.Optional;

public final class InterviewEndPolicy {

  public static final int MAX_ANSWERED_TURNS = 5;

  private InterviewEndPolicy() {
  }

  // 종료 판단은 LLM이 아니라 서버 룰이 한다
  public static Optional<InterviewEndReason> decide(TraceState state, int answeredTurnCount) {
    if (state.isChecklistComplete()) {
      return Optional.of(InterviewEndReason.CHECKLIST_COMPLETE);
    }
    if (answeredTurnCount >= MAX_ANSWERED_TURNS) {
      return Optional.of(InterviewEndReason.TURN_LIMIT);
    }
    return Optional.empty();
  }
}
