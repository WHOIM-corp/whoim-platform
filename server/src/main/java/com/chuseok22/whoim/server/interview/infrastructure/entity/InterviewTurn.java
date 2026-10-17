package com.chuseok22.whoim.server.interview.infrastructure.entity;

import com.chuseok22.whoim.server.common.infrastructure.persistence.BaseEntity;
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
public class InterviewTurn extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Interview interview;

  @Column(nullable = false)
  private int turnNumber;

  @Column(nullable = false)
  private String question;

  private String answer;

  // 턴에서 추출한 TRACE 조각의 JSON 문자열
  private String extractedTrace;

  @Builder
  private InterviewTurn(Interview interview, int turnNumber, String question) {
    this.interview = interview;
    this.turnNumber = turnNumber;
    this.question = question;
  }

  public void recordAnswer(String answer) {
    this.answer = answer;
  }

  public void recordExtraction(String extractedTraceJson) {
    this.extractedTrace = extractedTraceJson;
  }
}
