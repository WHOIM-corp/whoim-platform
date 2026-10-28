package com.chuseok22.whoim.server.handover.application.dto.response;

import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import com.chuseok22.whoim.server.handover.application.dto.data.HandoverProgress;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record HandoverPackageResponse(
    UUID id,
    String name,
    String description,
    HandoverStatus status,
    MemberResponse predecessor,
    MemberResponse successor,
    HandoverProgressResponse progress,
    List<HandoverCardSummaryResponse> cards,
    OffsetDateTime createdAt,
    OffsetDateTime publishedAt
) {

  public static HandoverPackageResponse from(
      HandoverPackage handoverPackage, List<TraceCard> cards, Map<UUID, EvidenceCounts> countsByCardId) {
    return new HandoverPackageResponse(
        handoverPackage.getId(),
        handoverPackage.getName(),
        handoverPackage.getDescription(),
        handoverPackage.getStatus(),
        MemberResponse.from(handoverPackage.getPredecessor()),
        handoverPackage.getSuccessor() == null ? null : MemberResponse.from(handoverPackage.getSuccessor()),
        HandoverProgressResponse.from(HandoverProgress.from(cards)),
        cards.stream()
            .map(card -> HandoverCardSummaryResponse.from(
                card, countsByCardId.getOrDefault(card.getId(), EvidenceCounts.ZERO)))
            .toList(),
        SeoulTime.toOffsetDateTime(handoverPackage.getCreatedAt()),
        handoverPackage.getPublishedAt() == null ? null : SeoulTime.toOffsetDateTime(handoverPackage.getPublishedAt()));
  }
}
