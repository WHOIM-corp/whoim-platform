package com.chuseok22.whoim.server.gap.application.dto.response;

import com.chuseok22.whoim.server.gap.core.constant.GapProgress;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import java.util.List;
import java.util.UUID;

public record GapResponse(
    UUID id,
    GapType type,
    String summary,
    String question,
    List<GapAnchorResponse> anchors,
    GapProgress progress
) {

  public static GapResponse from(Gap gap, List<GapAnchor> anchors, GapProgress progress) {
    return new GapResponse(
        gap.getId(),
        gap.getType(),
        gap.getSummary(),
        gap.getQuestion(),
        anchors.stream().map(GapAnchorResponse::from).toList(),
        progress);
  }
}
