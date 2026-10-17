package com.chuseok22.whoim.server.consultation.application.dto.data;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationEvidenceResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// 읽기 트랜잭션 안에서 만들어 LLM 호출 뒤에도 지연 로딩 없이 쓰는 후보 Card 스냅샷이다. label은 LLM이 Card를 가리키는 이름(C1, C2...)이다
public record ConsultationCandidate(
    String label,
    UUID cardId,
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
    OffsetDateTime predecessorConfirmedAt,
    OffsetDateTime approvedAt
) {

  public static ConsultationCandidate from(
      String label, TraceCard card, List<CardEvidenceLink> links, List<CardStateHistory> histories) {
    return new ConsultationCandidate(
        label,
        card.getId(),
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
        links.stream().map(ConsultationEvidenceResponse::from).toList(),
        lastReachedAt(histories, CardStatus.PREDECESSOR_CONFIRMED),
        lastReachedAt(histories, CardStatus.APPROVED));
  }

  // 이력은 시간순이므로 해당 상태로 마지막에 전이한 행의 시각을 쓴다
  private static OffsetDateTime lastReachedAt(List<CardStateHistory> histories, CardStatus status) {
    OffsetDateTime reachedAt = null;
    for (CardStateHistory history : histories) {
      if (history.getToStatus() == status) {
        reachedAt = SeoulTime.toOffsetDateTime(history.getCreatedAt());
      }
    }
    return reachedAt;
  }
}
