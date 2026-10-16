package com.chuseok22.whoim.server.card.application.dto.response;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CardResponse(
    UUID id,
    UUID interviewId,
    CardStatus status,
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
    List<CardEvidenceLinkResponse> evidenceLinks,
    OffsetDateTime createdAt
) {

  public static CardResponse from(TraceCard card, List<CardEvidenceLink> links) {
    return new CardResponse(
        card.getId(),
        card.getInterview().getId(),
        card.getStatus(),
        card.getTitle(),
        card.getSituation(),
        card.getSignal(),
        card.getCause(),
        card.getCondition(),
        card.getJudgment(),
        card.getAlternative(),
        card.getAction(),
        card.getReason(),
        card.getException(),
        card.getRisk(),
        links.stream().map(CardEvidenceLinkResponse::from).toList(),
        SeoulTime.toOffsetDateTime(card.getCreatedAt()));
  }
}
