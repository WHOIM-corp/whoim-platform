package com.chuseok22.whoim.server.card.infrastructure.entity;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
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
public class CardEvidenceLink extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private TraceCard card;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EvidenceLinkType type;

  // DIRECT일 때만 채워진다
  @ManyToOne(fetch = FetchType.LAZY)
  private InterviewTurn turn;

  // RECORDED일 때만 채워진다
  @ManyToOne(fetch = FetchType.LAZY)
  private EvidenceDocument document;

  private Integer startOffset;

  private Integer endOffset;

  @Column(nullable = false)
  private String excerpt;

  @Builder
  private CardEvidenceLink(TraceCard card, EvidenceLinkType type, InterviewTurn turn,
      EvidenceDocument document, Integer startOffset, Integer endOffset, String excerpt) {
    this.card = card;
    this.type = type;
    this.turn = turn;
    this.document = document;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
    this.excerpt = excerpt;
  }
}
