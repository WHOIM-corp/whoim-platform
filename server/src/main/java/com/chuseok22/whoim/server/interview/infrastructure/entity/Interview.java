package com.chuseok22.whoim.server.interview.infrastructure.entity;

import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
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
public class Interview extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private HandoverPackage handoverPackage;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Gap gap;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InterviewStatus status;

  @Enumerated(EnumType.STRING)
  private InterviewEndReason endReason;

  // TRACE 필드 채움 상태를 담은 JSON 문자열
  @Column(nullable = false)
  private String traceState;

  @Builder
  private Interview(HandoverPackage handoverPackage, Gap gap) {
    this.handoverPackage = handoverPackage;
    this.gap = gap;
    this.status = InterviewStatus.IN_PROGRESS;
    this.endReason = null;
    this.traceState = "{}";
  }

  public void updateTraceState(String traceStateJson) {
    this.traceState = traceStateJson;
  }

  public void complete(InterviewEndReason reason) {
    this.status = InterviewStatus.COMPLETED;
    this.endReason = reason;
  }
}
