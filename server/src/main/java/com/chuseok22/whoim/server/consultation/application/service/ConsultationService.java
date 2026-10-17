package com.chuseok22.whoim.server.consultation.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationCandidate;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationQuestionsResult;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationSelection;
import com.chuseok22.whoim.server.consultation.application.dto.data.ConsultationSuggestionResult;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationAnswerRequest;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationQuestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.request.ConsultationSuggestionsRequest;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationCardResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationQuestionsResponse;
import com.chuseok22.whoim.server.consultation.application.dto.response.ConsultationSuggestionsResponse;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 상담은 저장하지 않는 2단계(질문 -> 제시)이며 단계마다 LLM을 1번 호출한다.
// LLM 호출이 트랜잭션 안에 들어가지 않도록 @Transactional 대신 읽기 템플릿으로 후보만 읽는다
@Slf4j
@Service
public class ConsultationService {

  static final String NO_PUBLISHED_CARD = "참고할 공개된 Card가 없습니다.";
  static final String NO_RELATED_CARD = "관련 Card가 없습니다.";
  static final int MAX_QUESTIONS = 3;
  static final int MAX_SUGGESTIONS = 2;

  private final ConsultationCandidateFinder candidateFinder;
  private final CardEvidenceLinkRepository cardEvidenceLinkRepository;
  private final CardStateHistoryRepository cardStateHistoryRepository;
  private final LlmService llmService;
  private final TransactionTemplate readTransaction;

  public ConsultationService(
      ConsultationCandidateFinder candidateFinder,
      CardEvidenceLinkRepository cardEvidenceLinkRepository,
      CardStateHistoryRepository cardStateHistoryRepository,
      LlmService llmService,
      PlatformTransactionManager transactionManager) {
    this.candidateFinder = candidateFinder;
    this.cardEvidenceLinkRepository = cardEvidenceLinkRepository;
    this.cardStateHistoryRepository = cardStateHistoryRepository;
    this.llmService = llmService;
    this.readTransaction = new TransactionTemplate(transactionManager);
    this.readTransaction.setReadOnly(true);
  }

  public ConsultationQuestionsResponse questions(
      AuthenticatedMember requester, ConsultationQuestionsRequest request) {
    List<ConsultationCandidate> candidates =
        readTransaction.execute(status -> loadCandidates(requester.memberId()));
    if (candidates.isEmpty()) {
      return new ConsultationQuestionsResponse(List.of(), NO_PUBLISHED_CARD);
    }

    ConsultationQuestionsResult result = llmService.generate(
        ConsultationPrompt.questions(request.situation(), candidates), ConsultationQuestionsResult.class);

    List<String> questions = result.questions().stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(question -> !question.isEmpty())
        .limit(MAX_QUESTIONS)
        .toList();
    log.info("상담 질문 생성: candidates={}, questions={}", candidates.size(), questions.size());
    return new ConsultationQuestionsResponse(questions, null);
  }

  public ConsultationSuggestionsResponse suggest(
      AuthenticatedMember requester, ConsultationSuggestionsRequest request) {
    List<ConsultationCandidate> candidates =
        readTransaction.execute(status -> loadCandidates(requester.memberId()));
    if (candidates.isEmpty()) {
      return new ConsultationSuggestionsResponse(List.of(), NO_PUBLISHED_CARD);
    }

    List<ConsultationAnswerRequest> answers = request.answers() == null ? List.of() : request.answers();
    ConsultationSuggestionResult result = llmService.generate(
        ConsultationPrompt.suggestions(request.situation(), answers, candidates),
        ConsultationSuggestionResult.class);

    List<ConsultationCardResponse> cards = select(result.selections(), candidates);
    log.info("상담 Card 제시: candidates={}, selected={}", candidates.size(), cards.size());
    return new ConsultationSuggestionsResponse(cards, cards.isEmpty() ? NO_RELATED_CARD : null);
  }

  // 후보에 없는 label과 중복 label, 빈 설명은 버리고 상한까지만 제시한다
  private List<ConsultationCardResponse> select(
      List<ConsultationSelection> selections, List<ConsultationCandidate> candidates) {
    Map<String, ConsultationCandidate> candidatesByLabel = candidates.stream()
        .collect(Collectors.toMap(candidate -> normalizeLabel(candidate.label()), Function.identity()));
    Set<String> chosenLabels = new HashSet<>();
    List<ConsultationCardResponse> cards = new ArrayList<>();
    for (ConsultationSelection selection : selections) {
      if (cards.size() == MAX_SUGGESTIONS) {
        break;
      }
      if (selection == null || selection.cardLabel() == null || selection.explanation() == null
          || selection.explanation().isBlank()) {
        continue;
      }
      String label = normalizeLabel(selection.cardLabel());
      ConsultationCandidate candidate = candidatesByLabel.get(label);
      if (candidate != null && chosenLabels.add(label)) {
        cards.add(ConsultationCardResponse.from(candidate, selection.explanation().trim()));
      }
    }
    return cards;
  }

  // 모델이 "[c1]", "c1 "처럼 돌려줘도 같은 label로 본다
  private String normalizeLabel(String label) {
    return label.replaceAll("[\\s\\[\\]()]", "").toUpperCase(Locale.ROOT);
  }

  private List<ConsultationCandidate> loadCandidates(UUID successorId) {
    List<TraceCard> cards = candidateFinder.find(successorId);
    List<ConsultationCandidate> candidates = new ArrayList<>();
    for (int index = 0; index < cards.size(); index++) {
      TraceCard card = cards.get(index);
      candidates.add(ConsultationCandidate.from(
          "C" + (index + 1),
          card,
          cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card),
          cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)));
    }
    return candidates;
  }
}
