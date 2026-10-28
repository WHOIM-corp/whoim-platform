package com.chuseok22.whoim.server.handover.application.dto.response;

import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HandoverCardSummaryResponse(
    UUID id,
    String title,
    CardStatus status,
    String action,
    int directEvidenceCount,
    int recordedEvidenceCount,
    OffsetDateTime updatedAt
) {

  public static HandoverCardSummaryResponse from(TraceCard card, EvidenceCounts counts) {
    return new HandoverCardSummaryResponse(
        card.getId(),
        card.getTitle(),
        card.getStatus(),
        card.getAction(),
        counts.directCount(),
        counts.recordedCount(),
        SeoulTime.toOffsetDateTime(card.getUpdatedAt()));
  }
}
