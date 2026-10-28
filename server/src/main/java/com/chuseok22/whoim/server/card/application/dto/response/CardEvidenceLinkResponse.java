package com.chuseok22.whoim.server.card.application.dto.response;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import java.util.List;
import java.util.UUID;

// DIRECT는 turnNumber와 뒷받침하는 TRACE 요소(elements), RECORDED는 문서 정보와 글자 범위가 채워진다
public record CardEvidenceLinkResponse(
    EvidenceLinkType type,
    Integer turnNumber,
    UUID documentId,
    String fileName,
    Integer startOffset,
    Integer endOffset,
    String excerpt,
    List<TraceElement> elements
) {

  public static CardEvidenceLinkResponse from(CardEvidenceLink link, List<TraceElement> elements) {
    return new CardEvidenceLinkResponse(
        link.getType(),
        link.getTurn() == null ? null : link.getTurn().getTurnNumber(),
        link.getDocument() == null ? null : link.getDocument().getId(),
        link.getDocument() == null ? null : link.getDocument().getFileName(),
        link.getStartOffset(),
        link.getEndOffset(),
        link.getExcerpt(),
        elements);
  }
}
