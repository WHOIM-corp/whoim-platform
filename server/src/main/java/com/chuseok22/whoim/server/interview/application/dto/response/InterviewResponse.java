package com.chuseok22.whoim.server.interview.application.dto.response;

import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InterviewResponse(
    UUID id,
    UUID gapId,
    InterviewStatus status,
    InterviewEndReason endReason,
    String currentQuestion,
    List<InterviewTurnResponse> turns,
    Map<TraceElement, TraceStateEntry> traceState
) {

  // turns는 turnNumber 오름차순이어야 한다. 진행 중이고 마지막 턴이 미답변일 때만 currentQuestion을 낸다
  public static InterviewResponse from(
      Interview interview, List<InterviewTurn> turns, Map<TraceElement, TraceStateEntry> traceState) {
    return new InterviewResponse(
        interview.getId(),
        interview.getGap().getId(),
        interview.getStatus(),
        interview.getEndReason(),
        currentQuestionOf(interview, turns),
        turns.stream().map(InterviewTurnResponse::from).toList(),
        traceState);
  }

  private static String currentQuestionOf(Interview interview, List<InterviewTurn> turns) {
    if (interview.getStatus() != InterviewStatus.IN_PROGRESS || turns.isEmpty()) {
      return null;
    }
    InterviewTurn last = turns.get(turns.size() - 1);
    return last.getAnswer() == null ? last.getQuestion() : null;
  }
}
