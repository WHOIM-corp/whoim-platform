package com.chuseok22.whoim.server.interview.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.interview.application.dto.data.ExtractedElement;
import com.chuseok22.whoim.server.interview.application.dto.data.InterviewFirstQuestionResult;
import com.chuseok22.whoim.server.interview.application.dto.data.InterviewTurnResult;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewAnswerRequest;
import com.chuseok22.whoim.server.interview.application.dto.request.InterviewStartRequest;
import com.chuseok22.whoim.server.interview.application.dto.response.InterviewResponse;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewTurnRepository;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewServiceTest {

  private static final String ANSWER = "트래픽이 몰려 5회로 정했습니다";

  @Mock
  private InterviewRepository interviewRepository;
  @Mock
  private InterviewTurnRepository interviewTurnRepository;
  @Mock
  private GapRepository gapRepository;
  @Mock
  private GapAnchorRepository gapAnchorRepository;
  @Mock
  private HandoverPackageRepository handoverPackageRepository;
  @Mock
  private LlmService llmService;

  private final CountingTransactionManager transactionManager = new CountingTransactionManager();
  private final TraceStateCodec codec = new TraceStateCodec(JsonMapper.builder().build());
  private final List<InterviewTurn> savedTurns = new ArrayList<>();
  private final List<Interview> savedInterviews = new ArrayList<>();

  private InterviewService service;
  private Member owner;
  private HandoverPackage handoverPackage;
  private Gap gap;

  @BeforeEach
  void setUp() {
    service = new InterviewService(
        interviewRepository, interviewTurnRepository, gapRepository, gapAnchorRepository, handoverPackageRepository,
        llmService, codec, new TraceExtractionValidator(), transactionManager);
    owner = member("owner", Position.PREDECESSOR);
    handoverPackage = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    gap = Gap.builder().handoverPackage(handoverPackage).type(GapType.CHOICE_GAP).summary("요약").question("질문").build();
    ReflectionTestUtils.setField(gap, "id", UUID.randomUUID());
    GapAnchor anchor = GapAnchor.builder().gap(gap).excerpt("Retry 5회로 설정함").build();

    given(gapRepository.findById(gap.getId())).willReturn(Optional.of(gap));
    given(gapAnchorRepository.findAllByGap(gap)).willReturn(List.of(anchor));
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(interviewRepository.save(any(Interview.class))).willAnswer(invocation -> {
      Interview interview = invocation.getArgument(0);
      ReflectionTestUtils.setField(interview, "id", UUID.randomUUID());
      savedInterviews.add(interview);
      return interview;
    });
    given(interviewRepository.existsByGap(gap)).willAnswer(invocation -> !savedInterviews.isEmpty());
    given(interviewRepository.findById(any(UUID.class))).willAnswer(invocation -> findSaved(invocation.getArgument(0)));
    given(interviewRepository.findWithLockById(any(UUID.class))).willAnswer(invocation -> findSaved(invocation.getArgument(0)));
    given(interviewTurnRepository.save(any(InterviewTurn.class))).willAnswer(invocation -> {
      InterviewTurn turn = invocation.getArgument(0);
      if (!savedTurns.contains(turn)) {
        savedTurns.add(turn);
      }
      return turn;
    });
    given(interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(any(Interview.class)))
        .willAnswer(invocation -> List.copyOf(savedTurns));
    given(interviewTurnRepository.findTopByInterviewOrderByTurnNumberDesc(any(Interview.class)))
        .willAnswer(invocation -> savedTurns.isEmpty()
            ? Optional.empty() : Optional.of(savedTurns.get(savedTurns.size() - 1)));
    given(interviewTurnRepository.countByInterviewAndAnswerIsNotNull(any(Interview.class)))
        .willAnswer(invocation -> savedTurns.stream().filter(turn -> turn.getAnswer() != null).count());
    given(llmService.generate(any(LlmRequest.class), eq(InterviewFirstQuestionResult.class)))
        .willReturn(new InterviewFirstQuestionResult("첫 질문"));
  }

  // ---- start ----

  @Test
  void start_savesInterviewAndFirstTurn() {
    InterviewResponse response = service.start(requesterOf(owner), new InterviewStartRequest(gap.getId()));

    assertThat(response.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
    assertThat(response.gapId()).isEqualTo(gap.getId());
    assertThat(response.currentQuestion()).isEqualTo("첫 질문");
    assertThat(response.turns()).hasSize(1);
    assertThat(response.turns().get(0).turnNumber()).isEqualTo(1);
    assertThat(response.traceState()).isEmpty();
    assertThat(savedInterviews).hasSize(1);
    assertThat(savedTurns).hasSize(1);
  }

  @Test
  void start_llmFailureSavesNothing() {
    given(llmService.generate(any(LlmRequest.class), eq(InterviewFirstQuestionResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    assertThat(startAndCatch(requesterOf(owner)).getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);

    verify(interviewRepository, never()).save(any(Interview.class));
    verify(interviewTurnRepository, never()).save(any(InterviewTurn.class));
    verify(handoverPackageRepository, never()).findWithLockById(any());
  }

  @Test
  void start_rejectsExistingInterviewForGap() {
    given(interviewRepository.existsByGap(gap)).willReturn(true);

    assertThat(startAndCatch(requesterOf(owner)).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewFirstQuestionResult.class));
  }

  @Test
  void start_rejectsPublished() {
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);

    assertThat(startAndCatch(requesterOf(owner)).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewFirstQuestionResult.class));
  }

  @Test
  void start_rejectsNonOwner() {
    assertThat(startAndCatch(requesterOf(member("other", Position.PREDECESSOR))).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(startAndCatch(requesterOf(member("reviewer", Position.REVIEWER))).getErrorCode())
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewFirstQuestionResult.class));
  }

  @Test
  void start_rejectsUnknownGap() {
    UUID unknownGapId = UUID.randomUUID();
    given(gapRepository.findById(unknownGapId)).willReturn(Optional.empty());

    Throwable thrown = catchThrowable(() -> service.start(requesterOf(owner), new InterviewStartRequest(unknownGapId)));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void start_rejectsConflictAfterLock() {
    // 읽기 단계에서는 없었고, Package 잠금을 잡은 뒤에는 다른 요청이 이미 저장해 둔 상황
    given(interviewRepository.existsByGap(gap)).willReturn(false, true);

    assertThat(startAndCatch(requesterOf(owner)).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    verify(handoverPackageRepository).findWithLockById(handoverPackage.getId());
    verify(interviewRepository, never()).save(any(Interview.class));
    verify(interviewTurnRepository, never()).save(any(InterviewTurn.class));
  }

  @Test
  void start_callsLlmOutsideTransactionAndLocksInsideWrite() {
    AtomicInteger openDuringLlm = new AtomicInteger(-1);
    AtomicInteger openDuringLock = new AtomicInteger(-1);
    given(llmService.generate(any(LlmRequest.class), eq(InterviewFirstQuestionResult.class))).willAnswer(invocation -> {
      openDuringLlm.set(transactionManager.openTransactions());
      return new InterviewFirstQuestionResult("첫 질문");
    });
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willAnswer(invocation -> {
      openDuringLock.set(transactionManager.openTransactions());
      return Optional.of(handoverPackage);
    });

    service.start(requesterOf(owner), new InterviewStartRequest(gap.getId()));

    assertThat(openDuringLlm).hasValue(0);
    assertThat(openDuringLock).hasValue(1);
  }

  // ---- submitAnswer ----

  @Test
  void submitAnswer_savesNextTurnWhenIncomplete() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(
        turnResult("다음 질문", filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려")));

    InterviewResponse response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));

    assertThat(response.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
    assertThat(response.currentQuestion()).isEqualTo("다음 질문");
    assertThat(response.turns()).hasSize(2);
    assertThat(response.turns().get(0).answer()).isEqualTo(ANSWER);
    assertThat(response.turns().get(0).processed()).isTrue();
    assertThat(response.turns().get(1).turnNumber()).isEqualTo(2);
    assertThat(response.traceState().get(TraceElement.CAUSE).status()).isEqualTo(TraceElementStatus.FILLED);
    assertThat(response.traceState().get(TraceElement.CAUSE).source()).isEqualTo(EvidenceLinkType.DIRECT);
  }

  @Test
  void submitAnswer_endsWithChecklistComplete() {
    Interview interview = startInterview();
    ExtractedElement[] all = Arrays.stream(TraceElement.values())
        .map(element -> filled(element, element + " 내용", "DIRECT", "트래픽이 몰려"))
        .toArray(ExtractedElement[]::new);
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(turnResult("무시될 질문", all));

    InterviewResponse response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));

    assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
    assertThat(response.endReason()).isEqualTo(InterviewEndReason.CHECKLIST_COMPLETE);
    assertThat(response.currentQuestion()).isNull();
    assertThat(response.turns()).hasSize(1);
  }

  @Test
  void submitAnswer_endsWithTurnLimitAtFifthAnswer() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(
        turnResult("다음 질문", filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려")));

    InterviewResponse response = null;
    for (int answerCount = 1; answerCount <= 4; answerCount++) {
      response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));
      assertThat(response.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
    }
    response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));

    assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
    assertThat(response.endReason()).isEqualTo(InterviewEndReason.TURN_LIMIT);
    assertThat(response.turns()).hasSize(5);
    assertThat(response.currentQuestion()).isNull();
  }

  @Test
  void submitAnswer_fiveUnknownAnswersEndWithTurnLimit() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(
        turnResult("다음 질문", new ExtractedElement("CAUSE", "UNKNOWN", null, null, null)));

    InterviewResponse response = null;
    for (int answerCount = 1; answerCount <= 5; answerCount++) {
      response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf("모르겠습니다"));
    }

    assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
    assertThat(response.endReason()).isEqualTo(InterviewEndReason.TURN_LIMIT);
    assertThat(response.traceState().get(TraceElement.CAUSE).status()).isEqualTo(TraceElementStatus.UNKNOWN);
    assertThat(response.traceState().values().stream()
        .noneMatch(entry -> entry.status() == TraceElementStatus.FILLED)).isTrue();
  }

  @Test
  void submitAnswer_recordsUnknownWithoutInventingContent() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(
        turnResult("다음 질문", new ExtractedElement("RISK", "UNKNOWN", "지어낸 내용", "DIRECT", "지어낸 발췌")));

    InterviewResponse response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf("모르겠습니다"));

    assertThat(response.traceState().get(TraceElement.RISK).status()).isEqualTo(TraceElementStatus.UNKNOWN);
    assertThat(response.traceState().get(TraceElement.RISK).content()).isNull();
    assertThat(response.traceState().get(TraceElement.RISK).source()).isNull();
  }

  @Test
  void submitAnswer_dropsFabricatedExcerptButSucceeds() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(turnResult("다음 질문",
        filled(TraceElement.CAUSE, "지어낸 원인", "DIRECT", "답변에 없는 문장"),
        new ExtractedElement("REASON", "filled", "소문자 상태", "DIRECT", "트래픽이 몰려"),
        new ExtractedElement("NOT_AN_ELEMENT", "FILLED", "내용", "DIRECT", "트래픽이 몰려"),
        filled(TraceElement.SITUATION, "정상 추출", "DIRECT", "트래픽이 몰려")));

    InterviewResponse response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));

    assertThat(response.turns().get(0).processed()).isTrue();
    assertThat(response.traceState()).doesNotContainKey(TraceElement.CAUSE);
    assertThat(response.traceState()).doesNotContainKey(TraceElement.REASON);
    assertThat(response.traceState()).containsKey(TraceElement.SITUATION);
  }

  @Test
  void submitAnswer_keepsAnswerWhenLlmFails() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    assertThat(submitAndCatch(interview, ANSWER).getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);

    InterviewTurn pending = savedTurns.get(0);
    assertThat(pending.getAnswer()).isEqualTo(ANSWER);
    assertThat(pending.getExtractedTrace()).isNull();
    assertThat(savedTurns).hasSize(1);

    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(
        turnResult("다음 질문", filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려")));

    InterviewResponse retried = service.retry(requesterOf(owner), interview.getId());

    assertThat(retried.turns()).hasSize(2);
    assertThat(retried.turns().get(0).answer()).isEqualTo(ANSWER);
    assertThat(retried.turns().get(0).processed()).isTrue();
    assertThat(savedTurns.get(0).getExtractedTrace()).isNotNull();
    assertThat(retried.currentQuestion()).isEqualTo("다음 질문");
  }

  @Test
  void submitAnswer_blankNextQuestionWhileContinuingIs503AndRetryable() {
    Interview interview = startInterview();
    InterviewTurnResult blankQuestion = turnResult("   ",
        filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려"));
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(blankQuestion);

    assertThat(submitAndCatch(interview, ANSWER).getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class)))
        .willReturn(new InterviewTurnResult(List.of(), null));
    assertThat(catchRetry(interview).getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);

    // 답은 보존되고 추출·상태·다음 턴은 저장되지 않았다
    assertThat(savedTurns).hasSize(1);
    assertThat(savedTurns.get(0).getAnswer()).isEqualTo(ANSWER);
    assertThat(savedTurns.get(0).getExtractedTrace()).isNull();
    assertThat(interview.getTraceState()).isEqualTo("{}");
    assertThat(interview.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);

    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(
        turnResult("복구된 질문", filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려")));
    InterviewResponse retried = service.retry(requesterOf(owner), interview.getId());

    assertThat(retried.currentQuestion()).isEqualTo("복구된 질문");
    assertThat(retried.turns()).hasSize(2);
  }

  @Test
  void submitAnswer_blankNextQuestionIsIgnoredWhenInterviewEnds() {
    Interview interview = startInterview();
    ExtractedElement[] all = Arrays.stream(TraceElement.values())
        .map(element -> filled(element, element + " 내용", "DIRECT", "트래픽이 몰려"))
        .toArray(ExtractedElement[]::new);
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willReturn(turnResult(null, all));

    InterviewResponse response = service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));

    assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
  }

  @Test
  void submitAnswer_rejectsWhenPreviousTurnUnprocessed() {
    Interview interview = startInterview();
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));
    submitAndCatch(interview, ANSWER);

    CustomException exception = submitAndCatch(interview, "두 번째 답변");

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    assertThat(savedTurns.get(0).getAnswer()).isEqualTo(ANSWER);
  }

  @Test
  void submitAnswer_rejectsCompletedInterview() {
    Interview interview = startInterview();
    interview.complete(InterviewEndReason.TURN_LIMIT);

    assertThat(submitAndCatch(interview, ANSWER).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewTurnResult.class));
  }

  @Test
  void submitAnswer_rejectsPublished() {
    Interview interview = startInterview();
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);

    assertThat(submitAndCatch(interview, ANSWER).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    assertThat(savedTurns.get(0).getAnswer()).isNull();
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewTurnResult.class));
  }

  @Test
  void submitAnswer_rejectsNonOwner() {
    Interview interview = startInterview();

    Throwable thrown = catchThrowable(() -> service.submitAnswer(
        requesterOf(member("other", Position.PREDECESSOR)), interview.getId(), answerOf(ANSWER)));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(savedTurns.get(0).getAnswer()).isNull();
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewTurnResult.class));
  }

  @Test
  void submitAnswer_locksInterviewInsideWriteTransactionAndCallsLlmOutside() {
    Interview interview = startInterview();
    AtomicInteger openDuringLlm = new AtomicInteger(-1);
    AtomicInteger openDuringLock = new AtomicInteger(-1);
    given(interviewRepository.findWithLockById(interview.getId())).willAnswer(invocation -> {
      openDuringLock.set(transactionManager.openTransactions());
      return Optional.of(interview);
    });
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willAnswer(invocation -> {
      openDuringLlm.set(transactionManager.openTransactions());
      return turnResult("다음 질문", filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려"));
    });

    service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(ANSWER));

    assertThat(openDuringLock).hasValue(1);
    assertThat(openDuringLlm).hasValue(0);
  }

  // ---- retry ----

  @Test
  void retry_rejectsWhenNothingPending() {
    Interview interview = startInterview();

    // 마지막 턴에 답이 없음
    assertThat(catchRetry(interview).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    // 마지막 턴이 이미 처리됨
    savedTurns.get(0).recordAnswer(ANSWER);
    savedTurns.get(0).recordExtraction("{}");
    assertThat(catchRetry(interview).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    // 완료된 인터뷰
    interview.complete(InterviewEndReason.TURN_LIMIT);
    assertThat(catchRetry(interview).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewTurnResult.class));
  }

  @Test
  void retry_rejectsNonOwnerAndPublished() {
    Interview interview = startInterview();
    savedTurns.get(0).recordAnswer(ANSWER);

    Throwable denied = catchThrowable(() -> service.retry(requesterOf(member("other", Position.PREDECESSOR)), interview.getId()));
    assertThat(((CustomException) denied).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);

    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    assertThat(catchRetry(interview).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(InterviewTurnResult.class));
  }

  @Test
  void retry_concurrentProcessingConflictsAndSavesOnce() {
    Interview interview = startInterview();
    savedTurns.get(0).recordAnswer(ANSWER);
    // 이 요청이 LLM을 기다리는 동안 다른 요청이 같은 턴을 먼저 처리해 저장한 상황
    given(llmService.generate(any(LlmRequest.class), eq(InterviewTurnResult.class))).willAnswer(invocation -> {
      savedTurns.get(0).recordExtraction("{}");
      return turnResult("다음 질문", filled(TraceElement.CAUSE, "트래픽 급증", "DIRECT", "트래픽이 몰려"));
    });

    assertThat(catchRetry(interview).getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    assertThat(savedTurns).hasSize(1);
    assertThat(interview.getTraceState()).isEqualTo("{}");
    verify(interviewRepository).findWithLockById(interview.getId());
  }

  // ---- get ----

  @Test
  void get_ownerAndReviewerCanRead_otherPredecessorDenied() {
    Interview interview = startInterview();

    assertThat(service.get(requesterOf(owner), interview.getId()).id()).isEqualTo(interview.getId());
    assertThat(service.get(requesterOf(member("reviewer", Position.REVIEWER)), interview.getId()).turns()).hasSize(1);

    Throwable other = catchThrowable(() -> service.get(requesterOf(member("other", Position.PREDECESSOR)), interview.getId()));
    assertThat(((CustomException) other).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    Throwable successor = catchThrowable(() -> service.get(requesterOf(member("successor", Position.SUCCESSOR)), interview.getId()));
    assertThat(((CustomException) successor).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void get_unknownInterviewIs404() {
    Throwable thrown = catchThrowable(() -> service.get(requesterOf(owner), UUID.randomUUID()));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  // ---- helpers ----

  private Interview startInterview() {
    InterviewResponse response = service.start(requesterOf(owner), new InterviewStartRequest(gap.getId()));
    return findSaved(response.id()).orElseThrow();
  }

  private Optional<Interview> findSaved(UUID id) {
    return savedInterviews.stream().filter(interview -> interview.getId().equals(id)).findFirst();
  }

  private CustomException startAndCatch(AuthenticatedMember requester) {
    Throwable thrown = catchThrowable(() -> service.start(requester, new InterviewStartRequest(gap.getId())));
    assertThat(thrown).isInstanceOf(CustomException.class);
    return (CustomException) thrown;
  }

  private CustomException submitAndCatch(Interview interview, String answer) {
    Throwable thrown = catchThrowable(() -> service.submitAnswer(requesterOf(owner), interview.getId(), answerOf(answer)));
    assertThat(thrown).isInstanceOf(CustomException.class);
    return (CustomException) thrown;
  }

  private CustomException catchRetry(Interview interview) {
    Throwable thrown = catchThrowable(() -> service.retry(requesterOf(owner), interview.getId()));
    assertThat(thrown).isInstanceOf(CustomException.class);
    return (CustomException) thrown;
  }

  private static InterviewAnswerRequest answerOf(String answer) {
    return new InterviewAnswerRequest(answer);
  }

  private static InterviewTurnResult turnResult(String nextQuestion, ExtractedElement... extracted) {
    return new InterviewTurnResult(List.of(extracted), nextQuestion);
  }

  private static ExtractedElement filled(TraceElement element, String content, String source, String excerpt) {
    return new ExtractedElement(element.name(), "FILLED", content, source, excerpt);
  }

  private static Member member(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private static AuthenticatedMember requesterOf(Member member) {
    return new AuthenticatedMember(member.getId(), member.getUsername(), member.getRole(), member.getPosition());
  }

  // LLM 호출 시점에 열린 트랜잭션이 없는지 확인하기 위해 열린 개수를 센다
  private static final class CountingTransactionManager implements PlatformTransactionManager {

    private final AtomicInteger open = new AtomicInteger();

    int openTransactions() {
      return open.get();
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
      open.incrementAndGet();
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
      open.decrementAndGet();
    }

    @Override
    public void rollback(TransactionStatus status) {
      open.decrementAndGet();
    }
  }
}
