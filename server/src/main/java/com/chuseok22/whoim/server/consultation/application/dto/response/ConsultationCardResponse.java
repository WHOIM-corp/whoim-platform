package com.chuseok22.whoim.server.consultation.application.dto.response;

import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationCandidate;
import java.util.List;
import java.util.UUID;

// explanation은 LLM이 쓴 설명이고, 나머지는 승인된 Card에서 가져온 값이다
public record ConsultationCardResponse(
    UUID id,
    String title,
    String situation,
    String signal,
    String cause,
    String condition,
    String judgment,
    String alternative,
    String action,
    String reason,
    String exception,
    String risk,
    List<ConsultationEvidenceResponse> evidences,
    ConsultationVerificationResponse verification,
    String explanation
) {

  public static ConsultationCardResponse from(ConsultationCandidate candidate, String explanation) {
    return new ConsultationCardResponse(
        candidate.cardId(),
        candidate.title(),
        candidate.situation(),
        candidate.signal(),
        candidate.cause(),
        candidate.condition(),
        candidate.judgment(),
        candidate.alternative(),
        candidate.action(),
        candidate.reason(),
        candidate.exception(),
        candidate.risk(),
        candidate.evidences(),
        new ConsultationVerificationResponse(candidate.predecessorConfirmedAt(), candidate.approvedAt()),
        explanation);
  }
}
