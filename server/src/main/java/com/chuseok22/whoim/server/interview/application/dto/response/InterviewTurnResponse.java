package com.chuseok22.whoim.server.interview.application.dto.response;

import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;

// processed는 답변에 대한 LLM 추출이 저장됐는지(extracted_trace가 채워졌는지)를 뜻한다
public record InterviewTurnResponse(int turnNumber, String question, String answer, boolean processed) {

  public static InterviewTurnResponse from(InterviewTurn turn) {
    return new InterviewTurnResponse(
        turn.getTurnNumber(), turn.getQuestion(), turn.getAnswer(), turn.getExtractedTrace() != null);
  }
}
