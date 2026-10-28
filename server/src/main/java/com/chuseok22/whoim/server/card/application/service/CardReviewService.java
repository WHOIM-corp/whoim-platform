package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.application.dto.request.CardRejectRequest;
import com.chuseok22.whoim.server.card.application.dto.response.CardDetailResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardEvidenceLinkResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardSummaryResponse;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.interview.application.service.TraceStateCodec;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// LLM을 호출하는 CardService와 달리 DB만 쓰는 조회·상태 변경을 맡는다
@Service
@RequiredArgsConstructor
public class CardReviewService {

  private final TraceCardRepository traceCardRepository;
  private final CardEvidenceLinkRepository cardEvidenceLinkRepository;
  private final CardStateHistoryRepository cardStateHistoryRepository;
  private final MemberRepository memberRepository;
  private final TraceStateCodec traceStateCodec;
  private final EvidenceElementResolver evidenceElementResolver;
  private final CardEvidenceCounter cardEvidenceCounter;
  private final SuccessorCardPolicy successorCardPolicy;

  // status가 null이면 전체. 승인자에게는 DRAFT를 보이지 않는다
  @Transactional(readOnly = true)
  public List<CardSummaryResponse> list(AuthenticatedMember requester, CardStatus status) {
    List<TraceCard> cards;
    if (Position.REVIEWER == requester.position()) {
      cards = traceCardRepository.findAllByStatusNotOrderByUpdatedAtDesc(CardStatus.DRAFT);
    } else if (Position.PREDECESSOR == requester.position()) {
      cards = traceCardRepository.findAllByHandoverPackagePredecessorOrderByUpdatedAtDesc(
          findMember(requester));
    } else if (Position.SUCCESSOR == requester.position()) {
      // 후임자는 APPROVED만 본다. 다른 상태를 요청하면 빈 목록이다
      if (status != null && status != CardStatus.APPROVED) {
        return List.of();
      }
      cards = traceCardRepository
          .findAllForSuccessor(CardStatus.APPROVED, HandoverStatus.PUBLISHED, requester.memberId())
          .stream()
          .filter(card -> successorCardPolicy.isVisible(card, requester.memberId()))
          .toList();
    } else {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    List<TraceCard> filtered = cards.stream()
        .filter(card -> status == null || card.getStatus() == status)
        .toList();
    Map<UUID, EvidenceCounts> countsByCardId = cardEvidenceCounter.countFor(filtered);
    return filtered.stream()
        .map(card -> CardSummaryResponse.from(
            card, countsByCardId.getOrDefault(card.getId(), EvidenceCounts.ZERO)))
        .toList();
  }

  @Transactional(readOnly = true)
  public CardDetailResponse get(AuthenticatedMember requester, UUID cardId) {
    TraceCard card = findCard(cardId);
    if (Position.REVIEWER == requester.position()) {
      if (card.getStatus() == CardStatus.DRAFT) {
        throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
      }
    } else if (Position.PREDECESSOR == requester.position()) {
      validateOwner(requester, card);
    } else if (Position.SUCCESSOR == requester.position()) {
      // 존재를 알리지 않으려고 403이 아닌 404를 쓴다. 행위자 정보가 든 상태 이력은 비운다
      if (!successorCardPolicy.isVisible(card, requester.memberId())) {
        throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
      }
      return toDetail(card, List.of());
    } else {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    return toDetail(card);
  }

  // 검증 순서: Card(404) -> 소유(403) -> Package 상태(409) -> 전이(409)
  @Transactional
  public CardDetailResponse confirm(AuthenticatedMember requester, UUID cardId) {
    TraceCard card = findCard(cardId);
    validateOwner(requester, card);
    validatePackageWriting(card);
    return transition(card, findMember(requester), card.getStatus(), null, TraceCard::confirmByPredecessor);
  }

  // 검증 순서: Card(404) -> Package 상태(409) -> 근거(409) -> 전이(409)
  @Transactional
  public CardDetailResponse approve(AuthenticatedMember requester, UUID cardId) {
    TraceCard card = findCard(cardId);
    validatePackageWriting(card);
    if (!cardEvidenceLinkRepository.existsByCard(card)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    return transition(card, findMember(requester), card.getStatus(), null, TraceCard::approve);
  }

  // 검증 순서: Card(404) -> Package 상태(409) -> 전이(409). 사유는 상태 이력에 남는다
  @Transactional
  public CardDetailResponse reject(AuthenticatedMember requester, UUID cardId, CardRejectRequest request) {
    TraceCard card = findCard(cardId);
    validatePackageWriting(card);
    return transition(card, findMember(requester), card.getStatus(), request.reason(), TraceCard::reject);
  }

  private CardDetailResponse transition(
      TraceCard card, Member actor, CardStatus fromStatus, String reason, Consumer<TraceCard> change) {
    change.accept(card);
    cardStateHistoryRepository.save(CardStateHistory.builder()
        .card(card)
        .fromStatus(fromStatus)
        .toStatus(card.getStatus())
        .actor(actor)
        .reason(reason)
        .build());
    return toDetail(card);
  }

  private CardDetailResponse toDetail(TraceCard card) {
    return toDetail(card, cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card));
  }

  private CardDetailResponse toDetail(TraceCard card, List<CardStateHistory> histories) {
    TraceState state = traceStateCodec.read(card.getInterview().getTraceState());
    List<CardEvidenceLinkResponse> evidenceLinks = cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)
        .stream()
        .map(link -> CardEvidenceLinkResponse.from(link, evidenceElementResolver.resolve(state, link)))
        .toList();
    return CardDetailResponse.from(card, evidenceLinks, histories);
  }

  private TraceCard findCard(UUID cardId) {
    return traceCardRepository.findById(cardId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private Member findMember(AuthenticatedMember requester) {
    return memberRepository.findById(requester.memberId())
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private void validateOwner(AuthenticatedMember requester, TraceCard card) {
    if (!card.getHandoverPackage().getPredecessor().getId().equals(requester.memberId())) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
  }

  private void validatePackageWriting(TraceCard card) {
    if (card.getHandoverPackage().getStatus() != HandoverStatus.WRITING) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
  }
}
