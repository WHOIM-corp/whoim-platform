package com.chuseok22.whoim.server.gap.infrastructure.entity;

import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class GapAnchor extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Gap gap;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private EvidenceDocument document;

  @Column(nullable = false)
  private int startOffset;

  @Column(nullable = false)
  private int endOffset;

  @Column(nullable = false)
  private String excerpt;

  @Builder
  private GapAnchor(Gap gap, EvidenceDocument document, int startOffset, int endOffset, String excerpt) {
    this.gap = gap;
    this.document = document;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
    this.excerpt = excerpt;
  }
}
