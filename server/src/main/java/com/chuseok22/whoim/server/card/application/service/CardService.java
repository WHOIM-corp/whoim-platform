package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.data.CardGenerationResult;
import com.chuseok22.whoim.server.card.application.dto.data.DirectEvidenceCandidate;
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
import com.chuseok22.whoim.server.common.application.text.ExcerptMatcher;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.application.service.TraceStateCodec;
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
import com.chuseok22.whoim.server.member.core.constant.Position;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// LLM 호출이 트랜잭션 안에 들어가지 않도록 @Transactional 대신 읽기/쓰기 템플릿을 나눠 쓴다
// 로그에는 답변·prompt·LLM 응답 본문을 남기지 않는다
@Slf4j
@Service
public class CardService {

  private final InterviewRepository interviewRepository;
  private final InterviewTurnRepository interviewTurnRepository;
  private final GapAnchorRepository gapAnchorRepository;
  private final TraceCardRepository traceCardRepository;
  private final CardEvidenceLinkRepository cardEvidenceLinkRepository;
  private final CardStateHistoryRepository cardStateHistoryRepository;
  private final LlmService llmService;
  private final TraceStateCodec traceStateCodec;
  private final TransactionTemplate readTransaction;
  private final TransactionTemplate writeTransaction;

  public CardService(
      InterviewRepository interviewRepository,
      InterviewTurnRepository interviewTurnRepository,
      GapAnchorRepository gapAnchorRepository,
      TraceCardRepository traceCardRepository,
      CardEvidenceLinkRepository cardEvidenceLinkRepository,
      CardStateHistoryRepository cardStateHistoryRepository,
      LlmService llmService,
      TraceStateCodec traceStateCodec,
      PlatformTransactionManager transactionManager) {
    this.interviewRepository = interviewRepository;
    this.interviewTurnRepository = interviewTurnRepository;
    this.gapAnchorRepository = gapAnchorRepository;
    this.traceCardRepository = traceCardRepository;
    this.cardEvidenceLinkRepository = cardEvidenceLinkRepository;
    this.cardStateHistoryRepository = cardStateHistoryRepository;
    this.llmService = llmService;
    this.traceStateCodec = traceStateCodec;
    this.readTransaction = new TransactionTemplate(transactionManager);
    this.readTransaction.setReadOnly(true);
    this.writeTransaction = new TransactionTemplate(transactionManager);
  }

  // 검증 순서: 권한 -> 소유 -> Package 상태 -> 인터뷰 상태 -> Card 중복 -> DIRECT 근거 존재
  public CardResponse create(AuthenticatedMember requester, UUID interviewId) {
    LlmRequest request = readTransaction.execute(status -> prepareCreate(requester, interviewId));

    CardGenerationResult result = llmService.generate(request, CardGenerationResult.class);

    return writeTransaction.execute(status -> saveCard(interviewId, result));
  }

  private LlmRequest prepareCreate(AuthenticatedMember requester, UUID interviewId) {
    Interview interview = interviewRepository.findById(interviewId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    HandoverPackage handoverPackage = interview.getHandoverPackage();
    validateOwner(requester, handoverPackage);
    if (handoverPackage.getStatus() != HandoverStatus.WRITING
        || interview.getStatus() != InterviewStatus.COMPLETED
        || traceCardRepository.existsByInterview(interview)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    TraceState state = traceStateCodec.read(interview.getTraceState());
    // 재시도해도 같은 결과이므로 LLM을 부르지 않고 409로 끝낸다
    if (!state.hasDirectFilledElement()) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    List<InterviewTurn> turns = interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(interview);
    return CardGenerationPrompt.build(interview.getGap(), state, turns);
  }

  // 잠금 후 재검증: 같은 인터뷰의 Card 중복 생성을 막는다. 유효한 DIRECT 근거가 없으면 아무것도 저장하지 않고 503으로 롤백한다
  private CardResponse saveCard(UUID interviewId, CardGenerationResult result) {
    Interview interview = interviewRepository.findWithLockById(interviewId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (interview.getHandoverPackage().getStatus() != HandoverStatus.WRITING
        || interview.getStatus() != InterviewStatus.COMPLETED
        || traceCardRepository.existsByInterview(interview)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    TraceState state = traceStateCodec.read(interview.getTraceState());
    List<InterviewTurn> turns = interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(interview);
    Set<DirectLink> directLinks = verifyDirectEvidence(result.directEvidence(), state, turns);
    if (directLinks.isEmpty()) {
      log.warn("Card 생성 실패: 유효한 직접 근거 없음 interviewId={}", interviewId);
      throw new CustomException(ErrorCode.LLM_UNAVAILABLE);
    }

    Map<TraceElement, String> fields = resolveFields(result, state);
    TraceCard card = traceCardRepository.save(TraceCard.builder()
        .handoverPackage(interview.getHandoverPackage())
        .interview(interview)
        .title(result.title().trim())
        .situation(fields.get(TraceElement.SITUATION))
        .signal(fields.get(TraceElement.SIGNAL))
        .cause(fields.get(TraceElement.CAUSE))
        .condition(fields.get(TraceElement.CONDITION))
        .judgment(fields.get(TraceElement.JUDGMENT))
        .alternative(fields.get(TraceElement.ALTERNATIVE))
        .action(fields.get(TraceElement.ACTION))
        .reason(fields.get(TraceElement.REASON))
        .exception(fields.get(TraceElement.EXCEPTION))
        .risk(fields.get(TraceElement.RISK))
        .build());
    cardStateHistoryRepository.save(CardStateHistory.builder()
        .card(card).fromStatus(null).toStatus(CardStatus.DRAFT).actor(null).reason(null).build());

    List<CardEvidenceLink> links = new ArrayList<>();
    for (GapAnchor anchor : gapAnchorRepository.findAllByGap(interview.getGap())) {
      links.add(CardEvidenceLink.builder()
          .card(card)
          .type(EvidenceLinkType.RECORDED)
          .document(anchor.getDocument())
          .startOffset(anchor.getStartOffset())
          .endOffset(anchor.getEndOffset())
          .excerpt(anchor.getExcerpt())
          .build());
    }
    for (DirectLink directLink : directLinks) {
      links.add(CardEvidenceLink.builder()
          .card(card)
          .type(EvidenceLinkType.DIRECT)
          .turn(directLink.turn())
          .excerpt(directLink.excerpt())
          .build());
    }
    cardEvidenceLinkRepository.saveAll(links);
    log.info("Card 생성: cardId={}, interviewId={}, recordedLinks={}, directLinks={}",
        card.getId(), interviewId, links.size() - directLinks.size(), directLinks.size());
    return CardResponse.from(card, links);
  }

  private record DirectLink(InterviewTurn turn, String excerpt) {

  }

  // DIRECT FILLED 요소를 낸 턴의 답변에서 실제로 찾아지는 발췌만 인정하고, 저장은 답변 원문 구간으로 한다
  private Set<DirectLink> verifyDirectEvidence(
      List<DirectEvidenceCandidate> candidates, TraceState state, List<InterviewTurn> turns) {
    Set<Integer> directTurnNumbers = state.entries().values().stream()
        .filter(entry -> entry.status() == TraceElementStatus.FILLED && entry.source() == EvidenceLinkType.DIRECT)
        .map(TraceStateEntry::turnNumber)
        .collect(Collectors.toSet());
    Map<Integer, InterviewTurn> turnByNumber = turns.stream()
        .collect(Collectors.toMap(InterviewTurn::getTurnNumber, turn -> turn));

    Set<DirectLink> verified = new LinkedHashSet<>();
    for (DirectEvidenceCandidate candidate : candidates) {
      if (candidate.turnNumber() == null) {
        log.warn("Card 직접 근거 제외: 턴 번호 없음");
        continue;
      }
      InterviewTurn turn = turnByNumber.get(candidate.turnNumber());
      if (turn == null || !directTurnNumbers.contains(turn.getTurnNumber())) {
        log.warn("Card 직접 근거 제외: 근거를 낸 턴이 아님 turnNumber={}", candidate.turnNumber());
        continue;
      }
      Optional<String> matched = ExcerptMatcher.findIn(turn.getAnswer(), candidate.excerpt());
      if (matched.isEmpty()) {
        log.warn("Card 직접 근거 제외: 답변에서 발췌를 찾지 못함 turnNumber={}", candidate.turnNumber());
        continue;
      }
      verified.add(new DirectLink(turn, matched.get()));
    }
    return verified;
  }

  // FILLED가 아닌 요소의 필드는 LLM이 값을 줘도 null로 강제한다
  private Map<TraceElement, String> resolveFields(CardGenerationResult result, TraceState state) {
    Map<TraceElement, String> fields = new EnumMap<>(TraceElement.class);
    for (TraceElement element : TraceElement.values()) {
      TraceStateEntry entry = state.entries().get(element);
      boolean filled = entry != null && entry.status() == TraceElementStatus.FILLED;
      String value = filled ? valueOf(result, element) : null;
      if (value != null && !value.isBlank()) {
        fields.put(element, value.trim());
      }
    }
    return fields;
  }

  private String valueOf(CardGenerationResult result, TraceElement element) {
    return switch (element) {
      case SITUATION -> result.situation();
      case SIGNAL -> result.signal();
      case CAUSE -> result.cause();
      case CONDITION -> result.condition();
      case JUDGMENT -> result.judgment();
      case ALTERNATIVE -> result.alternative();
      case ACTION -> result.action();
      case REASON -> result.reason();
      case EXCEPTION -> result.exception();
      case RISK -> result.risk();
    };
  }

  private void validateOwner(AuthenticatedMember requester, HandoverPackage handoverPackage) {
    if (Position.PREDECESSOR != requester.position()
        || !handoverPackage.getPredecessor().getId().equals(requester.memberId())) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
  }
}
