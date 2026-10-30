package com.chuseok22.whoim.server.consultation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationQuestionsResult;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationSelection;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationSuggestionResult;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationAnswerRequest;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationQuestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationSuggestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationCardResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationQuestionsResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationSuggestionsResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

  @Mock
  private ConsultationCandidateFinder candidateFinder;
  @Mock
  private CardEvidenceLinkRepository cardEvidenceLinkRepository;
  @Mock
  private CardStateHistoryRepository cardStateHistoryRepository;
  @Mock
  private LlmService llmService;
  @Mock
  private SettingConfigService settingConfigService;

  private final CountingTransactionManager transactionManager = new CountingTransactionManager();

  private ConsultationService service;
  private AuthenticatedMember requester;

  @BeforeEach
  void setUp() {
    service = new ConsultationService(
        candidateFinder, cardEvidenceLinkRepository, cardStateHistoryRepository, llmService, settingConfigService,
        transactionManager);
    lenient().when(settingConfigService.getString(
        SettingKey.PROMPT_CONSULTATION_QUESTIONS, DefaultPromptInstructions.CONSULTATION_QUESTIONS))
        .thenReturn("질문 지침");
    lenient().when(settingConfigService.getString(
        SettingKey.PROMPT_CONSULTATION_SUGGESTIONS, DefaultPromptInstructions.CONSULTATION_SUGGESTIONS))
        .thenReturn("제시 지침");
    requester = new AuthenticatedMember(UUID.randomUUID(), "successor", Role.USER, Position.SUCCESSOR);
  }

  @Test
  void questions_withoutCandidates_skipsLlmAndExplains() {
    given(candidateFinder.find(requester.memberId())).willReturn(List.of());

    ConsultationQuestionsResponse response = service.questions(requester, new ConsultationQuestionsRequest("상황"));

    assertThat(response.questions()).isEmpty();
    assertThat(response.message()).isEqualTo(ConsultationService.NO_PUBLISHED_CARD);
    verifyNoInteractions(llmService);
  }

  @Test
  void questions_trimsBlankItemsAndLimitsToThree() {
    givenCandidates(givenCard("카드1"), givenCard("카드2"));
    given(llmService.generate(any(LlmRequest.class), eq(ConsultationQuestionsResult.class)))
        .willReturn(new ConsultationQuestionsResult(Arrays.asList(
            " CPU 사용률은 어떤가요? ", "", null, "DB 연결 수는요?", "   ", "PG 지연이 있나요?", "정산 배치는요?")));

    ConsultationQuestionsResponse response = service.questions(requester, new ConsultationQuestionsRequest("상황"));

    assertThat(response.questions()).containsExactly("CPU 사용률은 어떤가요?", "DB 연결 수는요?", "PG 지연이 있나요?");
    assertThat(response.message()).isNull();
    verify(llmService, times(1)).generate(any(LlmRequest.class), eq(ConsultationQuestionsResult.class));
  }

  @Test
  void questions_sendsLabeledCandidatesAndCallsLlmOutsideTransaction() {
    givenCandidates(givenCard("카드1"), givenCard("카드2"));
    AtomicInteger openTransactionsDuringLlmCall = new AtomicInteger(-1);
    given(llmService.generate(any(LlmRequest.class), eq(ConsultationQuestionsResult.class)))
        .willAnswer(invocation -> {
          openTransactionsDuringLlmCall.set(transactionManager.openTransactions());
          return new ConsultationQuestionsResult(List.of());
        });

    service.questions(requester, new ConsultationQuestionsRequest("상황"));

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(llmService).generate(captor.capture(), eq(ConsultationQuestionsResult.class));
    assertThat(captor.getValue().prompt()).contains("[C1] 카드1").contains("[C2] 카드2");
    assertThat(openTransactionsDuringLlmCall.get()).isZero();
    assertThat(transactionManager.readOnlyFlags()).containsExactly(true);
  }

  @Test
  void questions_passesConfiguredInstructionToPrompt() {
    givenCandidates(givenCard("카드1"));
    given(llmService.generate(any(LlmRequest.class), eq(ConsultationQuestionsResult.class)))
        .willReturn(new ConsultationQuestionsResult(List.of()));

    service.questions(requester, new ConsultationQuestionsRequest("상황"));

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(llmService).generate(captor.capture(), eq(ConsultationQuestionsResult.class));
    assertThat(captor.getValue().system()).startsWith("질문 지침");
  }

  @Test
  void suggest_passesConfiguredInstructionToPrompt() {
    givenCandidates(givenCard("카드1"));
    givenSelections();

    service.suggest(requester, suggestionsRequest(List.of()));

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(llmService).generate(captor.capture(), eq(ConsultationSuggestionResult.class));
    assertThat(captor.getValue().system()).startsWith("제시 지침");
  }

  @Test
  void suggest_withoutCandidates_skipsLlmAndExplains() {
    given(candidateFinder.find(requester.memberId())).willReturn(List.of());

    ConsultationSuggestionsResponse response = service.suggest(requester, suggestionsRequest(List.of()));

    assertThat(response.cards()).isEmpty();
    assertThat(response.message()).isEqualTo(ConsultationService.NO_PUBLISHED_CARD);
    verifyNoInteractions(llmService);
  }

  @Test
  void suggest_dropsUnknownAndDuplicateLabelsAndLimitsToTwo() {
    TraceCard first = givenCard("카드1");
    TraceCard second = givenCard("카드2");
    TraceCard third = givenCard("카드3");
    givenCandidates(first, second, third);
    givenSelections(
        new ConsultationSelection("C9", "후보에 없는 Card"),
        new ConsultationSelection("[c3]", " 정산 배치가 꺼진 상황과 같습니다 "),
        new ConsultationSelection("C3", "중복 선택"),
        new ConsultationSelection("C1", "일시 장애라고 판단했습니다"),
        new ConsultationSelection("C2", "상한을 넘는 Card"));

    ConsultationSuggestionsResponse response = service.suggest(requester, suggestionsRequest(List.of()));

    assertThat(response.cards()).extracting(ConsultationCardResponse::id)
        .containsExactly(third.getId(), first.getId());
    assertThat(response.cards()).extracting(ConsultationCardResponse::explanation)
        .containsExactly("정산 배치가 꺼진 상황과 같습니다", "일시 장애라고 판단했습니다");
    assertThat(response.message()).isNull();
  }

  @Test
  void suggest_skipsMalformedSelections() {
    TraceCard first = givenCard("카드1");
    givenCandidates(first);
    givenSelections(
        null,
        new ConsultationSelection(null, "label 없음"),
        new ConsultationSelection("C1", null),
        new ConsultationSelection("C1", "   "),
        new ConsultationSelection("C1", "유효한 설명"));

    ConsultationSuggestionsResponse response = service.suggest(requester, suggestionsRequest(List.of()));

    assertThat(response.cards()).extracting(ConsultationCardResponse::explanation).containsExactly("유효한 설명");
  }

  @Test
  void suggest_withoutRelatedSelection_explainsNoRelatedCard() {
    givenCandidates(givenCard("카드1"));
    givenSelections(new ConsultationSelection("C7", "후보 밖"));

    ConsultationSuggestionsResponse response = service.suggest(requester, suggestionsRequest(List.of()));

    assertThat(response.cards()).isEmpty();
    assertThat(response.message()).isEqualTo(ConsultationService.NO_RELATED_CARD);
  }

  @Test
  void suggest_treatsNullAnswersAsEmptyAndSendsAnswers() {
    givenCandidates(givenCard("카드1"));
    givenSelections();

    service.suggest(requester, new ConsultationSuggestionsRequest("상황", null));
    service.suggest(requester, suggestionsRequest(
        List.of(new ConsultationAnswerRequest("정산 배치가 실행 중인가요?", "아니요"))));

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(llmService, times(2)).generate(captor.capture(), eq(ConsultationSuggestionResult.class));
    assertThat(captor.getAllValues().get(0).prompt()).contains("(없음)");
    assertThat(captor.getAllValues().get(1).prompt()).contains("<answer>아니요</answer>");
  }

  @Test
  void suggest_exposesEvidenceSourcesAndVerificationWithoutOffsetsOrTurns() {
    TraceCard card = givenCard("카드1");
    givenCandidates(card);
    EvidenceDocument document = EvidenceDocument.builder()
        .fileName("incident-PAY-183.md").documentType(DocumentType.INCIDENT).content("내용").build();
    given(cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of(
        CardEvidenceLink.builder().card(card).type(EvidenceLinkType.RECORDED).document(document)
            .startOffset(3).endOffset(9).excerpt("업무 기록 발췌").build(),
        CardEvidenceLink.builder().card(card).type(EvidenceLinkType.DIRECT).excerpt("전임자 발언 발췌").build()));
    given(cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of(
        givenHistory(card, null, CardStatus.DRAFT, "2026-10-10T00:00:00Z"),
        givenHistory(card, CardStatus.DRAFT, CardStatus.PREDECESSOR_CONFIRMED, "2026-10-11T00:00:00Z"),
        givenHistory(card, CardStatus.PREDECESSOR_CONFIRMED, CardStatus.APPROVED, "2026-10-12T00:00:00Z")));
    givenSelections(new ConsultationSelection("C1", "설명"));

    ConsultationCardResponse response = service.suggest(requester, suggestionsRequest(List.of())).cards().get(0);

    assertThat(response.evidences()).extracting("type", "fileName", "excerpt").containsExactly(
        org.assertj.core.groups.Tuple.tuple(EvidenceLinkType.RECORDED, "incident-PAY-183.md", "업무 기록 발췌"),
        org.assertj.core.groups.Tuple.tuple(EvidenceLinkType.DIRECT, null, "전임자 발언 발췌"));
    assertThat(response.verification().predecessorConfirmedAt().toInstant())
        .isEqualTo(Instant.parse("2026-10-11T00:00:00Z"));
    assertThat(response.verification().approvedAt().toInstant()).isEqualTo(Instant.parse("2026-10-12T00:00:00Z"));
    assertThat(response.title()).isEqualTo("카드1");
    assertThat(response.explanation()).isEqualTo("설명");
    assertThat(Arrays.stream(response.evidences().get(0).getClass().getRecordComponents()).map(c -> c.getName()))
        .containsExactly("type", "fileName", "excerpt");
  }

  @Test
  void questions_propagatesLlmFailure() {
    givenCandidates(givenCard("카드1"));
    given(llmService.generate(any(LlmRequest.class), eq(ConsultationQuestionsResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    Throwable thrown = catchThrowable(() -> service.questions(requester, new ConsultationQuestionsRequest("상황")));

    assertThat(thrown).isInstanceOfSatisfying(CustomException.class,
        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
    verify(llmService, times(1)).generate(any(LlmRequest.class), eq(ConsultationQuestionsResult.class));
  }

  @Test
  void suggest_acceptsLabelsWrappedInSpacesAndParentheses() {
    TraceCard first = givenCard("카드1");
    TraceCard second = givenCard("카드2");
    givenCandidates(first, second);
    givenSelections(
        new ConsultationSelection("c1 ", "첫 번째 설명"),
        new ConsultationSelection("(C2)", "두 번째 설명"));

    ConsultationSuggestionsResponse response = service.suggest(requester, suggestionsRequest(List.of()));

    assertThat(response.cards()).extracting(ConsultationCardResponse::id)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  void suggest_propagatesLlmFailureWithoutPartialData() {
    givenCandidates(givenCard("카드1"));
    given(llmService.generate(any(LlmRequest.class), eq(ConsultationSuggestionResult.class)))
        .willThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE));

    Throwable thrown = catchThrowable(() -> service.suggest(requester, suggestionsRequest(List.of())));

    assertThat(thrown).isInstanceOfSatisfying(CustomException.class,
        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
    verify(llmService, times(1)).generate(any(LlmRequest.class), eq(ConsultationSuggestionResult.class));
  }

  private void givenCandidates(TraceCard... cards) {
    given(candidateFinder.find(requester.memberId())).willReturn(List.of(cards));
  }

  private void givenSelections(ConsultationSelection... selections) {
    given(llmService.generate(any(LlmRequest.class), eq(ConsultationSuggestionResult.class)))
        .willReturn(new ConsultationSuggestionResult(Arrays.asList(selections)));
  }

  private static ConsultationSuggestionsRequest suggestionsRequest(List<ConsultationAnswerRequest> answers) {
    return new ConsultationSuggestionsRequest("결제 성공률이 93%로 떨어졌어", new ArrayList<>(answers));
  }

  private static TraceCard givenCard(String title) {
    TraceCard card = TraceCard.builder().title(title).situation("상황").signal("신호").build();
    ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(card, "status", CardStatus.APPROVED);
    return card;
  }

  private static CardStateHistory givenHistory(
      TraceCard card, CardStatus from, CardStatus to, String createdAt) {
    CardStateHistory history = CardStateHistory.builder().card(card).fromStatus(from).toStatus(to).build();
    ReflectionTestUtils.setField(history, "createdAt", Instant.parse(createdAt));
    return history;
  }

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
