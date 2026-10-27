package com.chuseok22.whoim.server.handover.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.application.service.CardEvidenceCounter;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.application.dto.data.HandoverProgress;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageCreateRequest;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageSuccessorRequest;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandoverPackageService {

  private final HandoverPackageRepository handoverPackageRepository;
  private final MemberRepository memberRepository;
  private final TraceCardRepository traceCardRepository;
  private final CardEvidenceCounter cardEvidenceCounter;
  private final Clock clock;

  @Transactional
  public HandoverPackageResponse create(HandoverPackageCreateRequest request) {
    Member predecessor = findMemberWithPosition(request.predecessorId(), Position.PREDECESSOR);
    Member successor = request.successorId() == null
        ? null
        : findMemberWithPosition(request.successorId(), Position.SUCCESSOR);
    if (handoverPackageRepository.existsByPredecessorAndName(predecessor, request.name())) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }

    HandoverPackage handoverPackage = handoverPackageRepository.save(HandoverPackage.builder()
        .predecessor(predecessor)
        .successor(successor)
        .name(request.name())
        .description(request.description())
        .build());
    return HandoverPackageResponse.from(handoverPackage, List.of(), Map.of());
  }

  // 검증 순서: Package -> Package 상태 -> 후임자
  @Transactional
  public HandoverPackageResponse updateSuccessor(UUID handoverPackageId, HandoverPackageSuccessorRequest request) {
    HandoverPackage handoverPackage = handoverPackageRepository.findWithLockById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (handoverPackage.getStatus() == HandoverStatus.PUBLISHED) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    Member successor = request.successorId() == null
        ? null
        : findMemberWithPosition(request.successorId(), Position.SUCCESSOR);
    handoverPackage.assignSuccessor(successor);
    List<TraceCard> cards = traceCardRepository.findAllByHandoverPackage(handoverPackage);
    return HandoverPackageResponse.from(handoverPackage, cards, cardEvidenceCounter.countFor(cards));
  }

  @Transactional(readOnly = true)
  public List<HandoverPackageResponse> list(AuthenticatedMember requester) {
    List<HandoverPackage> handoverPackages;
    if (Position.REVIEWER == requester.position()) {
      handoverPackages = handoverPackageRepository.findAllByOrderByCreatedAtDesc();
    } else if (Position.PREDECESSOR == requester.position()) {
      Member predecessor = memberRepository.findById(requester.memberId())
          .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
      handoverPackages = handoverPackageRepository.findAllByPredecessorOrderByCreatedAtDesc(predecessor);
    } else if (Position.SUCCESSOR == requester.position()) {
      handoverPackages = handoverPackageRepository.findAllBySuccessorIdAndStatusOrderByCreatedAtDesc(
          requester.memberId(), HandoverStatus.PUBLISHED);
    } else {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    Map<HandoverPackage, List<TraceCard>> cardsByPackage = handoverPackages.isEmpty()
        ? Map.of()
        : traceCardRepository.findAllByHandoverPackageIn(handoverPackages).stream()
            .filter(card -> visibleTo(requester, card))
            .collect(Collectors.groupingBy(TraceCard::getHandoverPackage));
    Map<UUID, EvidenceCounts> countsByCardId = cardEvidenceCounter.countFor(
        cardsByPackage.values().stream().flatMap(List::stream).toList());
    return handoverPackages.stream()
        .map(handoverPackage -> HandoverPackageResponse.from(
            handoverPackage, cardsByPackage.getOrDefault(handoverPackage, List.of()), countsByCardId))
        .toList();
  }

  @Transactional(readOnly = true)
  public HandoverPackageResponse get(AuthenticatedMember requester, UUID handoverPackageId) {
    HandoverPackage handoverPackage = handoverPackageRepository.findById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    validateReadable(requester, handoverPackage);
    List<TraceCard> cards = traceCardRepository.findAllByHandoverPackage(handoverPackage).stream()
        .filter(card -> visibleTo(requester, card))
        .toList();
    return HandoverPackageResponse.from(handoverPackage, cards, cardEvidenceCounter.countFor(cards));
  }

  // 검증 순서: Package -> 이미 공개 -> 후임자 -> 승인 완료 여부
  @Transactional
  public HandoverPackageResponse publish(AuthenticatedMember requester, UUID handoverPackageId) {
    HandoverPackage handoverPackage = handoverPackageRepository.findById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (handoverPackage.getStatus() == HandoverStatus.PUBLISHED || handoverPackage.getSuccessor() == null) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    List<TraceCard> cards = traceCardRepository.findAllByHandoverPackage(handoverPackage);
    if (!HandoverProgress.from(cards).publishable()) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    Member publisher = memberRepository.findById(requester.memberId())
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    handoverPackage.publish(publisher, clock.instant());
    return HandoverPackageResponse.from(handoverPackage, cards, cardEvidenceCounter.countFor(cards));
  }

  private Member findMemberWithPosition(UUID memberId, Position expectedPosition) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (expectedPosition != member.getPosition()) {
      throw new CustomException(ErrorCode.INVALID_REQUEST);
    }
    return member;
  }

  // 후임자에게는 APPROVED Card만 보여 DRAFT·REJECTED 개수가 진행률로 새지 않게 한다
  private static boolean visibleTo(AuthenticatedMember requester, TraceCard card) {
    return Position.SUCCESSOR != requester.position() || card.getStatus() == CardStatus.APPROVED;
  }

  // 승인자는 전체, 전임자는 본인 Package, 후임자는 본인에게 지정된 PUBLISHED Package만
  private void validateReadable(AuthenticatedMember requester, HandoverPackage handoverPackage) {
    boolean reviewer = Position.REVIEWER == requester.position();
    boolean owningPredecessor = Position.PREDECESSOR == requester.position()
        && handoverPackage.getPredecessor().getId().equals(requester.memberId());
    boolean assignedSuccessor = Position.SUCCESSOR == requester.position()
        && handoverPackage.getStatus() == HandoverStatus.PUBLISHED
        && handoverPackage.getSuccessor() != null
        && handoverPackage.getSuccessor().getId().equals(requester.memberId());
    if (!reviewer && !owningPredecessor && !assignedSuccessor) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
  }
}
