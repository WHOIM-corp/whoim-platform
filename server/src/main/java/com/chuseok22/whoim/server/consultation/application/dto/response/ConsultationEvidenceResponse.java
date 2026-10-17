package com.chuseok22.whoim.server.consultation.application.dto.response;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;

// 후임자에게는 출처 종류, 업무 기록의 파일명, 발췌만 보여준다. 글자 범위와 턴 번호는 내보내지 않는다
public record ConsultationEvidenceResponse(
    EvidenceLinkType type,
    String fileName,
    String excerpt
) {

  public static ConsultationEvidenceResponse from(CardEvidenceLink link) {
    return new ConsultationEvidenceResponse(
        link.getType(),
        link.getDocument() == null ? null : link.getDocument().getFileName(),
        link.getExcerpt());
  }
}
