package com.chuseok22.whoim.server.card.infrastructure.entity;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
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
public class TraceCard extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private HandoverPackage handoverPackage;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Interview interview;

  @Column(nullable = false)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CardStatus status;

  private String situation;

  private String signal;

  private String cause;

  private String condition;

  private String judgment;

  private String alternative;

  private String action;

  private String reason;

  private String exception;

  private String risk;

  @Builder
  private TraceCard(HandoverPackage handoverPackage, Interview interview, String title,
      String situation, String signal, String cause, String condition, String judgment,
      String alternative, String action, String reason, String exception, String risk) {
    this.handoverPackage = handoverPackage;
    this.interview = interview;
    this.title = title;
    this.status = CardStatus.DRAFT;
    this.situation = situation;
    this.signal = signal;
    this.cause = cause;
    this.condition = condition;
    this.judgment = judgment;
    this.alternative = alternative;
    this.action = action;
    this.reason = reason;
    this.exception = exception;
    this.risk = risk;
  }

  public void confirmByPredecessor() {
    moveTo(CardStatus.PREDECESSOR_CONFIRMED);
  }

  public void approve() {
    moveTo(CardStatus.APPROVED);
  }

  private void moveTo(CardStatus next) {
    if (!status.canTransitionTo(next)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    this.status = next;
  }
}
