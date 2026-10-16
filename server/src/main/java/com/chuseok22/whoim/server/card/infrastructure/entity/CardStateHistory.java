package com.chuseok22.whoim.server.card.infrastructure.entity;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
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
public class CardStateHistory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private TraceCard card;

  @Enumerated(EnumType.STRING)
  private CardStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CardStatus toStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  private Member actor;

  private String reason;

  @Builder
  private CardStateHistory(TraceCard card, CardStatus fromStatus, CardStatus toStatus,
      Member actor, String reason) {
    this.card = card;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.actor = actor;
    this.reason = reason;
  }
}
