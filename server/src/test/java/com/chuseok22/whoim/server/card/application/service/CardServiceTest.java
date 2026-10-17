package com.chuseok22.whoim.server.card.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.data.CardGenerationResult;
import com.chuseok22.whoim.server.card.application.dto.data.DirectEvidenceCandidate;
import com.chuseok22.whoim.server.card.application.dto.response.CardEvidenceLinkResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardResponse;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.application.service.TraceStateCodec;
import com.chuseok22.whoim.server.interview.core.constant.InterviewEndReason;
import com.chuseok22.whoim.server.interview.core.constant.InterviewStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewTurnRepository;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CardServiceTest {

  private static final String ANSWER_ONE = "트래픽이 몰려서 재시도를 5회로 정했습니다";
  private static final String ANSWER_TWO = "문서에 적힌 대로 두었습니다";

  @Mock
  private InterviewRepository interviewRepository;
  @Mock
  private InterviewTurnRepository interviewTurnRepository;
  @Mock
  private GapAnchorRepository gapAnchorRepository;
  @Mock
  private TraceCardRepository traceCardRepository;
  @Mock
  private CardEvidenceLinkRepository cardEvidenceLinkRepository;
  @Mock
  private CardStateHistoryRepository cardStateHistoryRepository;
  @Mock
  private LlmService llmService;
  @Mock
  private SettingConfigService settingConfigService;

  private final CountingTransactionManager transactionManager = new CountingTransactionManager();
  private final TraceStateCodec codec = new TraceStateCodec(JsonMapper.builder().build());
  private final List<TraceCard> savedCards = new ArrayList<>();
  private final List<CardStateHistory> savedHistories = new ArrayList<>();
  private final List<CardEvidenceLink> savedLinks = new ArrayList<>();

  private CardService service;
  private Member owner;
  private HandoverPackage handoverPackage;
  private Gap gap;
  private Interview interview;
  private InterviewTurn turnOne;
  private InterviewTurn turnTwo;
  private Interview lockedInterview;
  private int openTransactionsDuringLlm = -1;
  private int openTransactionsAtLock = -1;

  @BeforeEach
  void setUp() {
    service = new CardService(
        interviewRepository, interviewTurnRepository, gapAnchorRepository, traceCardRepository,
        cardEvidenceLinkRepository, cardStateHistoryRepository, llmService, settingConfigService, codec, transactionManager);
    given(settingConfigService.getString(any(SettingKey.class), anyString()))
        .willAnswer(invocation -> invocation.getArgument(1));
    owner = member("owner", Position.PREDECESSOR);
    handoverPackage = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    gap = Gap.builder().handoverPackage(handoverPackage).type(GapType.CHOICE_GAP).summary("요약").question("질문").build();
    ReflectionTestUtils.setField(gap, "id", UUID.randomUUID());
    interview = Interview.builder().handoverPackage(handoverPackage).gap(gap).build();
    ReflectionTestUtils.setField(interview, "id", UUID.randomUUID());
    turnOne = turn(1, ANSWER_ONE);
    turnTwo = turn(2, ANSWER_TWO);

    // 턴 1은 DIRECT, 턴 2는 RECORDED로 채운 요소를 낸다. RISK는 UNKNOWN
    completeInterviewWith(Map.of(
        TraceElement.CAUSE, filled("트래픽 폭주", EvidenceLinkType.DIRECT, 1),
        TraceElement.ACTION, filled("재시도 5회 유지", EvidenceLinkType.RECORDED, 2),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 2)));

    EvidenceDocument document = EvidenceDocument.builder()
        .handoverPackage(handoverPackage).fileName("runbook.md").documentType(DocumentType.RUNBOOK).content("본문").build();
    ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
    GapAnchor anchor = GapAnchor.builder().gap(gap).document(document).startOffset(0).endOffset(2).excerpt("본문").build();

    given(interviewRepository.findById(interview.getId())).willReturn(Optional.of(interview));
    lockedInterview = Interview.builder().handoverPackage(handoverPackage).gap(gap).build();
    ReflectionTestUtils.setField(lockedInterview, "id", interview.getId());
    syncLockedInterview();
    given(interviewRepository.findWithLockById(interview.getId())).willAnswer(invocation -> {
      openTransactionsAtLock = transactionManager.openTransactions();
      syncLockedInterview();
      return Optional.of(lockedInterview);
    });
    given(interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(any(Interview.class)))
        .willReturn(List.of(turnOne, turnTwo));
    given(gapAnchorRepository.findAllByGap(gap)).willReturn(List.of(anchor));
    given(traceCardRepository.existsByInterview(any(Interview.class))).willAnswer(invocation -> !savedCards.isEmpty());
    given(traceCardRepository.save(any(TraceCard.class))).willAnswer(invocation -> {
      TraceCard card = invocation.getArgument(0);
      ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
      savedCards.add(card);
      return card;
    });
    given(cardStateHistoryRepository.save(any(CardStateHistory.class))).willAnswer(invocation -> {
      CardStateHistory history = invocation.getArgument(0);
      savedHistories.add(history);
      return history;
    });
    given(cardEvidenceLinkRepository.saveAll(anyList())).willAnswer(invocation -> {
      List<CardEvidenceLink> links = invocation.getArgument(0);
      savedLinks.addAll(links);
      return links;
    });
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willAnswer(invocation -> {
      openTransactionsDuringLlm = transactionManager.openTransactions();
      return resultWith(new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다"));
    });
  }

  @Test
  void create_usesDefaultInstructionWhenSettingIsAbsent() {
    service.create(requesterOf(owner), interview.getId());

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(llmService).generate(captor.capture(), eq(CardGenerationResult.class));
    assertThat(captor.getValue().system()).startsWith(DefaultPromptInstructions.CARD_GENERATION);
  }

  @Test
  void create_usesConfiguredInstructionWhenSettingExists() {
    given(settingConfigService.getString(SettingKey.PROMPT_CARD_GENERATION, DefaultPromptInstructions.CARD_GENERATION))
        .willReturn("커스텀 지침");

    service.create(requesterOf(owner), interview.getId());

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(llmService).generate(captor.capture(), eq(CardGenerationResult.class));
    assertThat(captor.getValue().system()).startsWith("커스텀 지침").contains("JSON만 출력");
  }

  @Test
  void create_savesDraftCardWithHistoryAndLinks() {
    CardResponse response = service.create(requesterOf(owner), interview.getId());

    verify(interviewRepository).findWithLockById(interview.getId());
    assertThat(openTransactionsAtLock).isEqualTo(1);
    assertThat(transactionManager.readOnlyFlags()).containsExactly(true, false);
    assertThat(savedCards.get(0).getInterview()).isSameAs(lockedInterview);
    assertThat(response.status()).isEqualTo(CardStatus.DRAFT);
    assertThat(response.interviewId()).isEqualTo(interview.getId());
    assertThat(response.title()).isEqualTo("재시도 5회 결정");
    assertThat(response.cause()).isEqualTo("트래픽 폭주");
    assertThat(savedCards).hasSize(1);
    assertThat(savedCards.get(0).getStatus()).isEqualTo(CardStatus.DRAFT);
    assertThat(savedHistories).hasSize(1);
    assertThat(savedHistories.get(0).getFromStatus()).isNull();
    assertThat(savedHistories.get(0).getToStatus()).isEqualTo(CardStatus.DRAFT);
    assertThat(savedHistories.get(0).getActor()).isNull();
    assertThat(savedHistories.get(0).getReason()).isNull();

    List<CardEvidenceLink> recorded = linksOf(EvidenceLinkType.RECORDED);
    assertThat(recorded).hasSize(1);
    assertThat(recorded.get(0).getExcerpt()).isEqualTo("본문");
    assertThat(recorded.get(0).getStartOffset()).isZero();
    assertThat(recorded.get(0).getEndOffset()).isEqualTo(2);
    assertThat(recorded.get(0).getDocument().getFileName()).isEqualTo("runbook.md");
    List<CardEvidenceLink> direct = linksOf(EvidenceLinkType.DIRECT);
    assertThat(direct).hasSize(1);
    assertThat(direct.get(0).getTurn()).isSameAs(turnOne);
    assertThat(direct.get(0).getExcerpt()).isEqualTo("재시도를 5회로 정했습니다");
    assertThat(response.evidenceLinks()).extracting(CardEvidenceLinkResponse::type)
        .containsExactlyInAnyOrder(EvidenceLinkType.RECORDED, EvidenceLinkType.DIRECT);
  }

  @Test
  void create_callsLlmOutsideTransaction() {
    service.create(requesterOf(owner), interview.getId());

    assertThat(openTransactionsDuringLlm).isZero();
  }

  @Test
  void create_forcesNullForUnfilledElements() {
    // LLM이 UNKNOWN(RISK)과 비어 있는 요소(SITUATION)에 값을 지어내도 저장하지 않는다
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(new CardGenerationResult(
        "제목", "지어낸 상황", "지어낸 신호", "트래픽 폭주", null, null, null, "재시도 5회 유지", null, null, "지어낸 위험",
        List.of(new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다"))));

    CardResponse response = service.create(requesterOf(owner), interview.getId());

    assertThat(response.situation()).isNull();
    assertThat(response.signal()).isNull();
    assertThat(response.risk()).isNull();
    assertThat(response.cause()).isEqualTo("트래픽 폭주");
    assertThat(response.action()).isEqualTo("재시도 5회 유지");
  }

  @Test
  void create_dropsDirectEvidenceNotInAnswer() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(resultWith(
        new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다"),
        new DirectEvidenceCandidate(1, "답변에 없는 지어낸 문장"),
        new DirectEvidenceCandidate(99, "트래픽이 몰려서")));

    service.create(requesterOf(owner), interview.getId());

    assertThat(linksOf(EvidenceLinkType.DIRECT)).extracting(CardEvidenceLink::getExcerpt)
        .containsExactly("재시도를 5회로 정했습니다");
  }

  @Test
  void create_dropsCandidatesWithNullTurnNumberOrBlankExcerpt() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(resultWith(
        new DirectEvidenceCandidate(null, "재시도를 5회로 정했습니다"),
        new DirectEvidenceCandidate(1, "  "),
        new DirectEvidenceCandidate(1, null),
        new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다")));

    service.create(requesterOf(owner), interview.getId());

    assertThat(linksOf(EvidenceLinkType.DIRECT)).extracting(CardEvidenceLink::getExcerpt)
        .containsExactly("재시도를 5회로 정했습니다");
  }

  @Test
  void create_dedupesSameDirectEvidence() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(resultWith(
        new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다"),
        new DirectEvidenceCandidate(1, "재시도를  5회로\n정했습니다")));

    service.create(requesterOf(owner), interview.getId());

    assertThat(linksOf(EvidenceLinkType.DIRECT)).hasSize(1);
  }

  @Test
  void create_failsWhenNoValidDirectEvidence() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(resultWith(
        new DirectEvidenceCandidate(1, "답변에 없는 지어낸 문장")));

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);

    assertNothingSaved();
  }

  @Test
  void create_rejectsWhenNoDirectFilledElementWithoutCallingLlm() {
    // 전부 UNKNOWN인 경우(5턴 내내 "모르겠다" -> TURN_LIMIT)
    completeInterviewWith(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 1),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 2)));
    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    // FILLED가 RECORDED뿐인 경우
    completeInterviewWith(Map.of(TraceElement.ACTION, filled("재시도 5회 유지", EvidenceLinkType.RECORDED, 2)));
    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    verify(llmService, never()).generate(any(LlmRequest.class), eq(CardGenerationResult.class));
    assertNothingSaved();
  }

  @Test
  void create_dropsDirectEvidenceFromTurnWithoutFilledDirectElement() {
    // 턴 2의 답변에는 있지만 턴 2는 DIRECT FILLED 요소를 내지 않았다
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(resultWith(
        new DirectEvidenceCandidate(2, "문서에 적힌 대로"),
        new DirectEvidenceCandidate(1, "트래픽이 몰려서")));

    service.create(requesterOf(owner), interview.getId());

    List<CardEvidenceLink> direct = linksOf(EvidenceLinkType.DIRECT);
    assertThat(direct).hasSize(1);
    assertThat(direct.get(0).getTurn()).isSameAs(turnOne);
  }

  @Test
  void create_toleratesWhitespaceDifferencesInExcerpt() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(resultWith(
        new DirectEvidenceCandidate(1, "트래픽이   몰려서\n재시도를 5회로")));

    service.create(requesterOf(owner), interview.getId());

    // 저장하는 발췌는 LLM 문자열이 아니라 답변 원문 구간이다
    assertThat(linksOf(EvidenceLinkType.DIRECT)).extracting(CardEvidenceLink::getExcerpt)
        .containsExactly("트래픽이 몰려서 재시도를 5회로");
  }

  @Test
  void create_rejectsIncompleteInterview() {
    ReflectionTestUtils.setField(interview, "status", InterviewStatus.IN_PROGRESS);

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(CardGenerationResult.class));
  }

  @Test
  void create_rejectsExistingCard() {
    given(traceCardRepository.existsByInterview(any(Interview.class))).willReturn(true);

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(CardGenerationResult.class));
  }

  @Test
  void create_secondCallIsConflictAndSavesOnce() {
    service.create(requesterOf(owner), interview.getId());

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    assertThat(savedCards).hasSize(1);
    assertThat(savedHistories).hasSize(1);
  }

  @Test
  void create_conflictAfterLockDiscardsResult() {
    // 읽기 단계에서는 없었고, Interview 잠금을 잡은 뒤에는 다른 요청이 이미 저장해 둔 상황
    given(traceCardRepository.existsByInterview(any(Interview.class))).willReturn(false, true);

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);

    verify(llmService).generate(any(LlmRequest.class), eq(CardGenerationResult.class));
    assertNothingSaved();
  }

  @Test
  void create_conflictWhenPublishedAfterLlmCall() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willAnswer(invocation -> {
      ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
      return resultWith(new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다"));
    });

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    assertNothingSaved();
  }

  @Test
  void create_llmFailureSavesNothing() {
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);

    verify(interviewRepository, never()).findWithLockById(any());
    assertNothingSaved();
  }

  @Test
  void create_rejectsNonOwner() {
    Throwable otherPredecessor = catchThrowable(
        () -> service.create(requesterOf(member("other", Position.PREDECESSOR)), interview.getId()));
    Throwable reviewer = catchThrowable(
        () -> service.create(requesterOf(member("reviewer", Position.REVIEWER)), interview.getId()));

    assertThat(((CustomException) otherPredecessor).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(((CustomException) reviewer).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(CardGenerationResult.class));
  }

  @Test
  void create_rejectsPublished() {
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);

    assertThat(createAndCatch().getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(llmService, never()).generate(any(LlmRequest.class), eq(CardGenerationResult.class));
  }

  @Test
  void create_mapsEveryElementToItsCardField() {
    Map<TraceElement, TraceStateEntry> all = new EnumMap<>(TraceElement.class);
    for (TraceElement element : TraceElement.values()) {
      all.put(element, filled("내용", EvidenceLinkType.DIRECT, 1));
    }
    completeInterviewWith(all);
    given(llmService.generate(any(LlmRequest.class), eq(CardGenerationResult.class))).willReturn(new CardGenerationResult(
        "  v-title  ", "v-situation", "v-signal", "v-cause", "v-condition", "v-judgment", "v-alternative", "v-action",
        "v-reason", "v-exception", "v-risk", List.of(new DirectEvidenceCandidate(1, "재시도를 5회로 정했습니다"))));

    service.create(requesterOf(owner), interview.getId());

    TraceCard card = savedCards.get(0);
    assertThat(card.getTitle()).isEqualTo("v-title");
    assertThat(card.getSituation()).isEqualTo("v-situation");
    assertThat(card.getSignal()).isEqualTo("v-signal");
    assertThat(card.getCause()).isEqualTo("v-cause");
    assertThat(card.getCondition()).isEqualTo("v-condition");
    assertThat(card.getJudgment()).isEqualTo("v-judgment");
    assertThat(card.getAlternative()).isEqualTo("v-alternative");
    assertThat(card.getAction()).isEqualTo("v-action");
    assertThat(card.getReason()).isEqualTo("v-reason");
    assertThat(card.getException()).isEqualTo("v-exception");
    assertThat(card.getRisk()).isEqualTo("v-risk");
  }

  @Test
  void create_nonOwnerOnPublishedPackageIsAccessDenied() {
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);

    Throwable thrown = catchThrowable(
        () -> service.create(requesterOf(member("other", Position.PREDECESSOR)), interview.getId()));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void create_rejectsUnknownInterview() {
    Throwable thrown = catchThrowable(() -> service.create(requesterOf(owner), UUID.randomUUID()));

    assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  private void completeInterviewWith(Map<TraceElement, TraceStateEntry> entries) {
    interview.updateTraceState(codec.write(new TraceState(new EnumMap<>(entries))));
    ReflectionTestUtils.setField(interview, "status", InterviewStatus.IN_PROGRESS);
    interview.complete(InterviewEndReason.TURN_LIMIT);
  }

  // 잠금 조회가 돌려주는 인스턴스는 findById와 다르므로, 상태를 맞춰 두어야 같은 인터뷰로 보인다
  private void syncLockedInterview() {
    lockedInterview.updateTraceState(interview.getTraceState());
    ReflectionTestUtils.setField(lockedInterview, "status", interview.getStatus());
    ReflectionTestUtils.setField(lockedInterview, "endReason", interview.getEndReason());
  }

  private CustomException createAndCatch() {
    Throwable thrown = catchThrowable(() -> service.create(requesterOf(owner), interview.getId()));
    assertThat(thrown).isInstanceOf(CustomException.class);
    return (CustomException) thrown;
  }

  private void assertNothingSaved() {
    verify(traceCardRepository, never()).save(any(TraceCard.class));
    verify(cardStateHistoryRepository, never()).save(any(CardStateHistory.class));
    verify(cardEvidenceLinkRepository, never()).saveAll(anyList());
  }

  private List<CardEvidenceLink> linksOf(EvidenceLinkType type) {
    return savedLinks.stream().filter(link -> link.getType() == type).toList();
  }

  private static CardGenerationResult resultWith(DirectEvidenceCandidate... evidence) {
    return new CardGenerationResult(
        "재시도 5회 결정", null, null, "트래픽 폭주", null, null, null, "재시도 5회 유지", null, null, null,
        List.of(evidence));
  }

  private static TraceStateEntry filled(String content, EvidenceLinkType source, int turnNumber) {
    return new TraceStateEntry(TraceElementStatus.FILLED, content, source, turnNumber);
  }

  private InterviewTurn turn(int number, String answer) {
    InterviewTurn turn = InterviewTurn.builder().interview(interview).turnNumber(number).question("질문").build();
    ReflectionTestUtils.setField(turn, "id", UUID.randomUUID());
    turn.recordAnswer(answer);
    return turn;
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
    private final List<Boolean> readOnlyFlags = new ArrayList<>();

    List<Boolean> readOnlyFlags() {
      return readOnlyFlags;
    }

    int openTransactions() {
      return open.get();
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
      open.incrementAndGet();
      readOnlyFlags.add(definition.isReadOnly());
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
