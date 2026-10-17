package com.chuseok22.whoim.server.gap.application.dto.response;

import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import java.util.UUID;

public record GapAnchorResponse(
    UUID documentId,
    String fileName,
    int startOffset,
    int endOffset,
    String excerpt
) {

  public static GapAnchorResponse from(GapAnchor anchor) {
    return new GapAnchorResponse(
        anchor.getDocument().getId(),
        anchor.getDocument().getFileName(),
        anchor.getStartOffset(),
        anchor.getEndOffset(),
        anchor.getExcerpt());
  }
}
