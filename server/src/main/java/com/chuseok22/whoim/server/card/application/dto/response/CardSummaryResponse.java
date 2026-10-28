package com.chuseok22.whoim.server.card.application.dto.response;

import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CardSummaryResponse(
    UUID id,
    UUID handoverPackageId,
    UUID interviewId,
    String title,
    CardStatus status,
    String action,
    int directEvidenceCount,
    int recordedEvidenceCount,
    OffsetDateTime updatedAt
) {

  public static CardSummaryResponse from(TraceCard card, EvidenceCounts counts) {
    return new CardSummaryResponse(
        card.getId(),
        card.getHandoverPackage().getId(),
        card.getInterview().getId(),
        card.getTitle(),
        card.getStatus(),
        card.getAction(),
        counts.directCount(),
        counts.recordedCount(),
        SeoulTime.toOffsetDateTime(card.getUpdatedAt()));
  }
}
