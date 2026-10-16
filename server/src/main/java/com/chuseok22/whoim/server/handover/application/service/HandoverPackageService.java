package com.chuseok22.whoim.server.handover.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageCreateRequest;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageSuccessorRequest;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandoverPackageService {

  private final HandoverPackageRepository handoverPackageRepository;
  private final MemberRepository memberRepository;

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
    return HandoverPackageResponse.from(handoverPackage);
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
    return HandoverPackageResponse.from(handoverPackage);
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
    } else {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    return handoverPackages.stream().map(HandoverPackageResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public HandoverPackageResponse get(AuthenticatedMember requester, UUID handoverPackageId) {
    HandoverPackage handoverPackage = handoverPackageRepository.findById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    validateReadable(requester, handoverPackage);
    return HandoverPackageResponse.from(handoverPackage);
  }

  private Member findMemberWithPosition(UUID memberId, Position expectedPosition) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (expectedPosition != member.getPosition()) {
      throw new CustomException(ErrorCode.INVALID_REQUEST);
    }
    return member;
  }

  // EvidenceDocumentService.validateReadable과 같은 규칙: 승인자는 전체, 전임자는 본인 Package만
  private void validateReadable(AuthenticatedMember requester, HandoverPackage handoverPackage) {
    boolean reviewer = Position.REVIEWER == requester.position();
    boolean owningPredecessor = Position.PREDECESSOR == requester.position()
        && handoverPackage.getPredecessor().getId().equals(requester.memberId());
    if (!reviewer && !owningPredecessor) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
  }
}
