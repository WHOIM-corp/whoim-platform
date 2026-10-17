package com.chuseok22.whoim.server.handover.application.dto.response;

import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.application.dto.response.MemberResponse;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HandoverPackageResponse(
    UUID id,
    String name,
    String description,
    HandoverStatus status,
    MemberResponse predecessor,
    MemberResponse successor,
    OffsetDateTime createdAt
) {

  public static HandoverPackageResponse from(HandoverPackage handoverPackage) {
    return new HandoverPackageResponse(
        handoverPackage.getId(),
        handoverPackage.getName(),
        handoverPackage.getDescription(),
        handoverPackage.getStatus(),
        MemberResponse.from(handoverPackage.getPredecessor()),
        handoverPackage.getSuccessor() == null ? null : MemberResponse.from(handoverPackage.getSuccessor()),
        SeoulTime.toOffsetDateTime(handoverPackage.getCreatedAt()));
  }
}
