package com.chuseok22.whoim.server.interview.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.interview.application.dto.data.InterviewFirstQuestionResult;
import com.chuseok22.whoim.server.interview.application.dto.data.InterviewTurnResult;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewAnswerRequest;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewStartRequest;
import com.chuseok22.whoim.server.interview.application.dto.response.InterviewResponse;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndPolicy;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewTurnRepository;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import com.chuseok22.whoim.server.member.core.constant.Position;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// LLM 호출이 트랜잭션 안에 들어가지 않도록 @Transactional 대신 읽기/쓰기 템플릿을 나눠 쓴다
// 로그에는 답변·prompt·LLM 응답 본문을 남기지 않는다
@Slf4j
@Service
public class InterviewService {

  private final InterviewRepository interviewRepository;
  private final InterviewTurnRepository interviewTurnRepository;
  private final GapRepository gapRepository;
  private final GapAnchorRepository gapAnchorRepository;
  private final HandoverPackageRepository handoverPackageRepository;
  private final LlmService llmService;
  private final TraceStateCodec traceStateCodec;
  private final TraceExtractionValidator traceExtractionValidator;
  private final TransactionTemplate readTransaction;
  private final TransactionTemplate writeTransaction;

  public InterviewService(
      InterviewRepository interviewRepository,
      InterviewTurnRepository interviewTurnRepository,
      GapRepository gapRepository,
      GapAnchorRepository gapAnchorRepository,
      HandoverPackageRepository handoverPackageRepository,
      LlmService llmService,
      TraceStateCodec traceStateCodec,
      TraceExtractionValidator traceExtractionValidator,
      PlatformTransactionManager transactionManager) {
    this.interviewRepository = interviewRepository;
    this.interviewTurnRepository = interviewTurnRepository;
    this.gapRepository = gapRepository;
    this.gapAnchorRepository = gapAnchorRepository;
    this.handoverPackageRepository = handoverPackageRepository;
    this.llmService = llmService;
    this.traceStateCodec = traceStateCodec;
    this.traceExtractionValidator = traceExtractionValidator;
    this.readTransaction = new TransactionTemplate(transactionManager);
    this.readTransaction.setReadOnly(true);
    this.writeTransaction = new TransactionTemplate(transactionManager);
  }

  private record StartContext(UUID gapId, UUID handoverPackageId, LlmRequest request) {

  }

  private record PendingTurnContext(
      LlmRequest request, String answer, List<String> anchorExcerpts, int turnNumber) {

  }

  // 검증 순서: 권한 -> 소유 -> Package 상태 -> 인터뷰 중복
  public InterviewResponse start(AuthenticatedMember requester, InterviewStartRequest request) {
    StartContext context = readTransaction.execute(status -> prepareStart(requester, request.gapId()));

    InterviewFirstQuestionResult first = llmService.generate(context.request(), InterviewFirstQuestionResult.class);

    return writeTransaction.execute(status -> saveStart(context, first.question()));
  }

  public InterviewResponse submitAnswer(AuthenticatedMember requester, UUID interviewId, InterviewAnswerRequest request) {
    writeTransaction.executeWithoutResult(status -> recordAnswer(requester, interviewId, request.answer()));
    return processPendingTurn(interviewId);
  }

  public InterviewResponse retry(AuthenticatedMember requester, UUID interviewId) {
    readTransaction.executeWithoutResult(status -> {
      Interview interview = findInterview(interviewId);
      validateWritable(requester, interview);
      requirePendingTurn(interview);
    });
    return processPendingTurn(interviewId);
  }

  public InterviewResponse get(AuthenticatedMember requester, UUID interviewId) {
    return readTransaction.execute(status -> {
      Interview interview = findInterview(interviewId);
      validateReadable(requester, interview);
      return toResponse(interview);
    });
  }

  private StartContext prepareStart(AuthenticatedMember requester, UUID gapId) {
    Gap gap = gapRepository.findById(gapId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    HandoverPackage handoverPackage = gap.getHandoverPackage();
    validateOwner(requester, handoverPackage);
    if (handoverPackage.getStatus() != HandoverStatus.WRITING || interviewRepository.existsByGap(gap)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    List<GapAnchor> anchors = gapAnchorRepository.findAllByGap(gap);
    return new StartContext(gap.getId(), handoverPackage.getId(), InterviewPrompt.firstQuestion(gap, anchors));
  }

  // 아직 Interview 행이 없어 잠글 수 없으므로 Package 행을 잠가 같은 Gap의 중복 시작을 막는다
  private InterviewResponse saveStart(StartContext context, String firstQuestion) {
    HandoverPackage handoverPackage = handoverPackageRepository.findWithLockById(context.handoverPackageId())
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    Gap gap = gapRepository.findById(context.gapId())
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (handoverPackage.getStatus() != HandoverStatus.WRITING || interviewRepository.existsByGap(gap)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    Interview interview = interviewRepository.save(Interview.builder().handoverPackage(handoverPackage).gap(gap).build());
    InterviewTurn firstTurn = interviewTurnRepository.save(
        InterviewTurn.builder().interview(interview).turnNumber(1).question(firstQuestion).build());
    log.info("인터뷰 시작: interviewId={}, gapId={}", interview.getId(), gap.getId());
    return InterviewResponse.from(interview, List.of(firstTurn), Map.of());
  }

  // 검증 순서: 권한 -> 소유 -> Package 상태 -> 인터뷰 상태 -> 마지막 턴
  private void recordAnswer(AuthenticatedMember requester, UUID interviewId, String answer) {
    Interview interview = interviewRepository.findWithLockById(interviewId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    validateWritable(requester, interview);
    InterviewTurn lastTurn = interviewTurnRepository.findTopByInterviewOrderByTurnNumberDesc(interview)
        .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    if (lastTurn.getAnswer() != null) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    lastTurn.recordAnswer(answer);
    interviewTurnRepository.save(lastTurn);
  }

  private InterviewResponse processPendingTurn(UUID interviewId) {
    PendingTurnContext context = readTransaction.execute(status -> preparePendingTurn(interviewId));

    InterviewTurnResult result = llmService.generate(context.request(), InterviewTurnResult.class);

    return writeTransaction.execute(status -> saveExtraction(interviewId, context, result));
  }

  private PendingTurnContext preparePendingTurn(UUID interviewId) {
    Interview interview = findInterview(interviewId);
    InterviewTurn pendingTurn = requirePendingTurn(interview);
    Gap gap = interview.getGap();
    List<GapAnchor> anchors = gapAnchorRepository.findAllByGap(gap);
    TraceState state = traceStateCodec.read(interview.getTraceState());
    int remainingTurns = InterviewEndPolicy.MAX_ANSWERED_TURNS
        - (int) interviewTurnRepository.countByInterviewAndAnswerIsNotNull(interview);
    LlmRequest request = InterviewPrompt.turn(
        gap, anchors, state, pendingTurn.getQuestion(), pendingTurn.getAnswer(), remainingTurns);
    return new PendingTurnContext(
        request,
        pendingTurn.getAnswer(),
        anchors.stream().map(GapAnchor::getExcerpt).toList(),
        pendingTurn.getTurnNumber());
  }

  // 잠금 후 재검증: 같은 턴의 중복 처리를 막는다. nextQuestion이 비면 아무것도 저장하지 않고 503으로 롤백한다
  private InterviewResponse saveExtraction(UUID interviewId, PendingTurnContext context, InterviewTurnResult result) {
    Interview interview = interviewRepository.findWithLockById(interviewId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (interview.getHandoverPackage().getStatus() != HandoverStatus.WRITING) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    InterviewTurn pendingTurn = requirePendingTurn(interview);
    if (pendingTurn.getTurnNumber() != context.turnNumber()) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }

    Map<TraceElement, TraceStateEntry> accepted = traceExtractionValidator.accept(
        result.extracted(), context.answer(), context.anchorExcerpts(), context.turnNumber());
    TraceState merged = traceStateCodec.read(interview.getTraceState()).merge(accepted);
    Optional<InterviewEndReason> endReason = InterviewEndPolicy.decide(
        merged, (int) interviewTurnRepository.countByInterviewAndAnswerIsNotNull(interview));
    if (endReason.isEmpty() && (result.nextQuestion() == null || result.nextQuestion().isBlank())) {
      log.warn("인터뷰 다음 질문 없음: interviewId={}, turnNumber={}", interviewId, context.turnNumber());
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }

    pendingTurn.recordExtraction(traceStateCodec.write(new TraceState(accepted)));
    interviewTurnRepository.save(pendingTurn);
    interview.updateTraceState(traceStateCodec.write(merged));
    if (endReason.isPresent()) {
      interview.complete(endReason.get());
      log.info("인터뷰 종료: interviewId={}, reason={}", interviewId, endReason.get());
    } else {
      interviewTurnRepository.save(InterviewTurn.builder()
          .interview(interview)
          .turnNumber(context.turnNumber() + 1)
          .question(result.nextQuestion())
          .build());
    }
    interviewRepository.save(interview);
    return toResponse(interview);
  }

  private Interview findInterview(UUID interviewId) {
    return interviewRepository.findById(interviewId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  // 진행 중이고 마지막 턴에 답이 있으며 아직 추출 전인 턴을 돌려준다. 아니면 409
  private InterviewTurn requirePendingTurn(Interview interview) {
    if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    InterviewTurn lastTurn = interviewTurnRepository.findTopByInterviewOrderByTurnNumberDesc(interview)
        .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    if (lastTurn.getAnswer() == null || lastTurn.getExtractedTrace() != null) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    return lastTurn;
  }

  private void validateWritable(AuthenticatedMember requester, Interview interview) {
    HandoverPackage handoverPackage = interview.getHandoverPackage();
    validateOwner(requester, handoverPackage);
    if (handoverPackage.getStatus() != HandoverStatus.WRITING || interview.getStatus() != InterviewStatus.IN_PROGRESS) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
  }

  private void validateOwner(AuthenticatedMember requester, HandoverPackage handoverPackage) {
    if (Position.PREDECESSOR != requester.position()
        || !handoverPackage.getPredecessor().getId().equals(requester.memberId())) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
  }

  private void validateReadable(AuthenticatedMember requester, Interview interview) {
    if (Position.REVIEWER == requester.position()) {
      return;
    }
    validateOwner(requester, interview.getHandoverPackage());
  }

  private InterviewResponse toResponse(Interview interview) {
    List<InterviewTurn> turns = interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(interview);
    return InterviewResponse.from(interview, turns, traceStateCodec.read(interview.getTraceState()).entries());
  }
}
