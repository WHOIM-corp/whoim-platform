package com.chuseok22.whoim.server.handover.application.dto.response;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.UUID;

public record HandoverCardSummaryResponse(
    UUID id,
    String title,
    CardStatus status
) {

  public static HandoverCardSummaryResponse from(TraceCard card) {
    return new HandoverCardSummaryResponse(card.getId(), card.getTitle(), card.getStatus());
  }
}
