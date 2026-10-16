package com.chuseok22.whoim.server.evidence.infrastructure.entity;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvidenceDocument extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private HandoverPackage handoverPackage;

  @Column(nullable = false)
  private String fileName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentType documentType;

  @Column(nullable = false)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Member uploadedBy;

  @Builder
  private EvidenceDocument(HandoverPackage handoverPackage, String fileName, DocumentType documentType, String content, Member uploadedBy) {
    this.handoverPackage = handoverPackage;
    this.fileName = fileName;
    this.documentType = documentType;
    this.content = content;
    this.uploadedBy = uploadedBy;
  }

  // 글자 범위는 시작 포함·끝 제외이며 정규화된 content의 String 인덱스(UTF-16) 기준이다
  public String excerpt(int startOffset, int endOffset) {
    if (startOffset < 0 || startOffset >= endOffset || endOffset > content.length()) {
      throw new CustomException(ErrorCode.EVIDENCE_EXCERPT_RANGE_INVALID);
    }
    return content.substring(startOffset, endOffset);
  }
}
