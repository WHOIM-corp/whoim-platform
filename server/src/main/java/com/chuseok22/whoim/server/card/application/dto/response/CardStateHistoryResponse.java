package com.chuseok22.whoim.server.card.application.dto.response;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import java.time.OffsetDateTime;

// 생성 시점 이력처럼 fromStatus, actor, reason이 없을 수 있다
public record CardStateHistoryResponse(
    CardStatus fromStatus,
    CardStatus toStatus,
    String actorUsername,
    String actorName,
    String reason,
    OffsetDateTime createdAt
) {

  public static CardStateHistoryResponse from(CardStateHistory history) {
    return new CardStateHistoryResponse(
        history.getFromStatus(),
        history.getToStatus(),
        history.getActor() == null ? null : history.getActor().getUsername(),
        history.getActor() == null ? null : history.getActor().getName(),
        history.getReason(),
        SeoulTime.toOffsetDateTime(history.getCreatedAt()));
  }
}
