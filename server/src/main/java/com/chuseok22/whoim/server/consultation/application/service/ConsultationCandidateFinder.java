package com.chuseok22.whoim.server.consultation.application.service;

import com.chuseok22.whoim.server.card.application.service.SuccessorCardPolicy;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 후임자 상담의 Hard Rule을 한 곳에 둔다. LLM은 이 후보 안에서만 고른다
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationCandidateFinder {

  // 프롬프트 크기를 제한하려고 최신 수정 순으로 자른다
  static final int MAX_CANDIDATES = 6;

  private final TraceCardRepository traceCardRepository;
  // 상태 없는 규칙이라 주입하지 않고 직접 만든다
  private final SuccessorCardPolicy successorCardPolicy = new SuccessorCardPolicy();

  // 읽기 트랜잭션 안에서 호출해야 한다
  public List<TraceCard> find(UUID successorId) {
    List<TraceCard> eligible = traceCardRepository
        .findAllForSuccessor(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successorId)
        .stream()
        .filter(card -> successorCardPolicy.isVisible(card, successorId))
        .toList();
    if (eligible.size() > MAX_CANDIDATES) {
      log.debug("상담 후보를 상한으로 잘랐습니다: total={}, limit={}", eligible.size(), MAX_CANDIDATES);
    }
    return eligible.stream().limit(MAX_CANDIDATES).toList();
  }
}
