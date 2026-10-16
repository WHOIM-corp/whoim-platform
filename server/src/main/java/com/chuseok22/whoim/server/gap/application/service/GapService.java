package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import com.chuseok22.whoim.server.gap.core.constant.GapProgress;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GapService {

  private final HandoverPackageRepository handoverPackageRepository;
  private final GapRepository gapRepository;
  private final GapAnchorRepository gapAnchorRepository;
  private final InterviewRepository interviewRepository;
  private final TraceCardRepository traceCardRepository;

  @Transactional(readOnly = true)
  public List<GapResponse> list(AuthenticatedMember requester, UUID handoverPackageId) {
    HandoverPackage handoverPackage = handoverPackageRepository.findById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    validateReadable(requester, handoverPackage);

    List<Gap> gaps = gapRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage);
    Map<UUID, Interview> interviewByGapId = interviewRepository.findAllByHandoverPackage(handoverPackage).stream()
        .collect(Collectors.toMap(interview -> interview.getGap().getId(), Function.identity()));
    Map<UUID, TraceCard> cardByInterviewId = traceCardRepository.findAllByHandoverPackage(handoverPackage).stream()
        .collect(Collectors.toMap(card -> card.getInterview().getId(), Function.identity()));
    Map<UUID, List<GapAnchor>> anchorsByGapId = gapAnchorRepository.findAllByGapIn(gaps).stream()
        .collect(Collectors.groupingBy(anchor -> anchor.getGap().getId()));

    return gaps.stream()
        .map(gap -> {
          Interview interview = interviewByGapId.get(gap.getId());
          TraceCard card = interview == null ? null : cardByInterviewId.get(interview.getId());
          return GapResponse.from(gap, anchorsByGapId.getOrDefault(gap.getId(), List.of()), computeProgress(interview, card));
        })
        .toList();
  }

  // 반려된 Card가 인터뷰 종료 사유보다 우선한다
  private GapProgress computeProgress(Interview interview, TraceCard card) {
    if (interview == null) {
      return GapProgress.NOT_STARTED;
    }
    if (card != null && card.getStatus() == CardStatus.REJECTED) {
      return GapProgress.REJECTED;
    }
    if (interview.getStatus() == InterviewStatus.IN_PROGRESS) {
      return GapProgress.IN_PROGRESS;
    }
    return interview.getEndReason() == InterviewEndReason.TURN_LIMIT ? GapProgress.PARTIAL : GapProgress.COMPLETED;
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
