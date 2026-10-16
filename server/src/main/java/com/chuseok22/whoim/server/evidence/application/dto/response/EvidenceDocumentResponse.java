package com.chuseok22.whoim.server.evidence.application.dto.response;

import com.chuseok22.whoim.server.common.application.time.SeoulTime;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EvidenceDocumentResponse(
    UUID id,
    String fileName,
    DocumentType documentType,
    int characterCount,
    UUID uploaderId,
    String uploaderName,
    OffsetDateTime uploadedAt
) {

  public static EvidenceDocumentResponse from(EvidenceDocument document) {
    return new EvidenceDocumentResponse(
        document.getId(),
        document.getFileName(),
        document.getDocumentType(),
        document.getContent().length(),
        document.getUploadedBy().getId(),
        document.getUploadedBy().getName(),
        SeoulTime.toOffsetDateTime(document.getCreatedAt()));
  }
}
