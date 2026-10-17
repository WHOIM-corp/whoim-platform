package com.chuseok22.whoim.server.card.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.dto.request.CardRejectRequest;
import com.chuseok22.whoim.server.card.application.dto.response.CardDetailResponse;
import com.chuseok22.whoim.server.card.application.dto.response.CardSummaryResponse;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CardReviewServiceTest {

  @Mock
  private TraceCardRepository traceCardRepository;
  @Mock
  private CardEvidenceLinkRepository cardEvidenceLinkRepository;
  @Mock
  private CardStateHistoryRepository cardStateHistoryRepository;
  @Mock
  private MemberRepository memberRepository;

  private CardReviewService service;

  private Member predecessor;
  private Member reviewer;
  private HandoverPackage handoverPackage;

  @BeforeEach
  void setUp() {
    service = new CardReviewService(
        traceCardRepository, cardEvidenceLinkRepository, cardStateHistoryRepository, memberRepository);
    predecessor = givenMember("predecessor", Position.PREDECESSOR);
    reviewer = givenMember("reviewer", Position.REVIEWER);
    handoverPackage = HandoverPackage.builder().predecessor(predecessor).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
  }

  @Test
  void confirm_movesDraftAndSavesHistory() {
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));

    CardDetailResponse response = service.confirm(requesterOf(predecessor), card.getId());

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    assertThat(response.status()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    ArgumentCaptor<CardStateHistory> captor = ArgumentCaptor.forClass(CardStateHistory.class);
    verify(cardStateHistoryRepository).save(captor.capture());
    assertThat(captor.getValue().getFromStatus()).isEqualTo(CardStatus.DRAFT);
    assertThat(captor.getValue().getToStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    assertThat(captor.getValue().getActor()).isSameAs(predecessor);
    assertThat(captor.getValue().getReason()).isNull();
  }

  @Test
  void confirm_rejectsOtherPredecessor() {
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    Member other = givenMember("other", Position.PREDECESSOR);

    assertErrorCode(ErrorCode.ACCESS_DENIED, () -> service.confirm(requesterOf(other), card.getId()));

    assertThat(card.getStatus()).isEqualTo(CardStatus.DRAFT);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void confirm_rejectsUnknownCard() {
    UUID unknownId = UUID.randomUUID();
    given(traceCardRepository.findById(unknownId)).willReturn(Optional.empty());

    assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND, () -> service.confirm(requesterOf(predecessor), unknownId));

    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void confirm_rejectsNonDraftCard() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));

    assertErrorCode(ErrorCode.STATE_CONFLICT, () -> service.confirm(requesterOf(predecessor), card.getId()));

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void confirm_rejectsPublishedPackage() {
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));

    assertErrorCode(ErrorCode.STATE_CONFLICT, () -> service.confirm(requesterOf(predecessor), card.getId()));

    assertThat(card.getStatus()).isEqualTo(CardStatus.DRAFT);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void approve_movesConfirmedAndSavesHistory() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.existsByCard(card)).willReturn(true);
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    CardDetailResponse response = service.approve(requesterOf(reviewer), card.getId());

    assertThat(card.getStatus()).isEqualTo(CardStatus.APPROVED);
    assertThat(response.status()).isEqualTo(CardStatus.APPROVED);
    ArgumentCaptor<CardStateHistory> captor = ArgumentCaptor.forClass(CardStateHistory.class);
    verify(cardStateHistoryRepository).save(captor.capture());
    assertThat(captor.getValue().getFromStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    assertThat(captor.getValue().getToStatus()).isEqualTo(CardStatus.APPROVED);
    assertThat(captor.getValue().getActor()).isSameAs(reviewer);
  }

  @Test
  void approve_rejectsCardWithoutEvidence() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.existsByCard(card)).willReturn(false);

    assertErrorCode(ErrorCode.STATE_CONFLICT, () -> service.approve(requesterOf(reviewer), card.getId()));

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void approve_rejectsDraftCard() {
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.existsByCard(card)).willReturn(true);
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    assertErrorCode(ErrorCode.STATE_CONFLICT, () -> service.approve(requesterOf(reviewer), card.getId()));

    assertThat(card.getStatus()).isEqualTo(CardStatus.DRAFT);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void approve_rejectsPublishedPackage() {
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));

    assertErrorCode(ErrorCode.STATE_CONFLICT, () -> service.approve(requesterOf(reviewer), card.getId()));

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void approve_rejectsUnknownCard() {
    UUID unknownId = UUID.randomUUID();
    given(traceCardRepository.findById(unknownId)).willReturn(Optional.empty());

    assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND, () -> service.approve(requesterOf(reviewer), unknownId));
  }

  @Test
  void reject_movesConfirmedAndSavesHistoryWithReason() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    CardDetailResponse response = service.reject(
        requesterOf(reviewer), card.getId(), new CardRejectRequest("근거가 부족합니다"));

    assertThat(card.getStatus()).isEqualTo(CardStatus.REJECTED);
    assertThat(response.status()).isEqualTo(CardStatus.REJECTED);
    ArgumentCaptor<CardStateHistory> captor = ArgumentCaptor.forClass(CardStateHistory.class);
    verify(cardStateHistoryRepository).save(captor.capture());
    assertThat(captor.getValue().getFromStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    assertThat(captor.getValue().getToStatus()).isEqualTo(CardStatus.REJECTED);
    assertThat(captor.getValue().getActor()).isSameAs(reviewer);
    assertThat(captor.getValue().getReason()).isEqualTo("근거가 부족합니다");
  }

  @Test
  void reject_rejectsCardsThatAreNotConfirmed() {
    for (CardStatus status : new CardStatus[] {
        CardStatus.DRAFT, CardStatus.APPROVED, CardStatus.REJECTED}) {
      TraceCard card = givenCard(status);
      given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
      given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

      assertErrorCode(ErrorCode.STATE_CONFLICT,
          () -> service.reject(requesterOf(reviewer), card.getId(), new CardRejectRequest("사유")));

      assertThat(card.getStatus()).isEqualTo(status);
    }
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void reject_rejectsPublishedPackage() {
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));

    assertErrorCode(ErrorCode.STATE_CONFLICT,
        () -> service.reject(requesterOf(reviewer), card.getId(), new CardRejectRequest("사유")));

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void reject_rejectsUnknownReviewer() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.empty());

    assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND,
        () -> service.reject(requesterOf(reviewer), card.getId(), new CardRejectRequest("사유")));

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void reject_rejectsUnknownCard() {
    UUID unknownId = UUID.randomUUID();
    given(traceCardRepository.findById(unknownId)).willReturn(Optional.empty());

    assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND,
        () -> service.reject(requesterOf(reviewer), unknownId, new CardRejectRequest("사유")));

    verify(cardStateHistoryRepository, never()).save(any());
  }

  @Test
  void list_forPredecessor_returnsOwnCardsFilteredByStatus() {
    TraceCard draft = givenCard(CardStatus.DRAFT);
    TraceCard confirmed = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(traceCardRepository.findAllByHandoverPackagePredecessorOrderByUpdatedAtDesc(predecessor))
        .willReturn(List.of(draft, confirmed));

    List<CardSummaryResponse> all = service.list(requesterOf(predecessor), null);
    List<CardSummaryResponse> onlyDraft = service.list(requesterOf(predecessor), CardStatus.DRAFT);

    assertThat(all).extracting(CardSummaryResponse::id).containsExactly(draft.getId(), confirmed.getId());
    assertThat(onlyDraft).extracting(CardSummaryResponse::id).containsExactly(draft.getId());
  }

  @Test
  void list_forReviewer_excludesDraftAndReturnsEmptyForDraftFilter() {
    TraceCard confirmed = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findAllByStatusNotOrderByUpdatedAtDesc(CardStatus.DRAFT))
        .willReturn(List.of(confirmed));

    List<CardSummaryResponse> all = service.list(requesterOf(reviewer), null);
    List<CardSummaryResponse> onlyDraft = service.list(requesterOf(reviewer), CardStatus.DRAFT);

    assertThat(all).extracting(CardSummaryResponse::id).containsExactly(confirmed.getId());
    assertThat(onlyDraft).isEmpty();
  }

  @Test
  void list_rejectsOtherPositions() {
    Member successor = givenMember("successor", Position.SUCCESSOR);

    assertErrorCode(ErrorCode.ACCESS_DENIED, () -> service.list(requesterOf(successor), null));
  }

  @Test
  void get_forOwningPredecessor_returnsDetail() {
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of());
    given(cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of());

    CardDetailResponse response = service.get(requesterOf(predecessor), card.getId());

    assertThat(response.id()).isEqualTo(card.getId());
    assertThat(response.handoverPackageId()).isEqualTo(handoverPackage.getId());
  }

  @Test
  void get_rejectsOtherPredecessor() {
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    Member other = givenMember("other", Position.PREDECESSOR);

    assertErrorCode(ErrorCode.ACCESS_DENIED, () -> service.get(requesterOf(other), card.getId()));
  }

  @Test
  void get_hidesDraftFromReviewer() {
    TraceCard card = givenCard(CardStatus.DRAFT);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));

    assertErrorCode(ErrorCode.RESOURCE_NOT_FOUND, () -> service.get(requesterOf(reviewer), card.getId()));
  }

  @Test
  void get_showsNonDraftToReviewer() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of());
    given(cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of());

    CardDetailResponse response = service.get(requesterOf(reviewer), card.getId());

    assertThat(response.status()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
  }

  private static void assertErrorCode(ErrorCode expected, Runnable action) {
    Throwable thrown = catchThrowable(action::run);
    assertThat(thrown).isInstanceOfSatisfying(CustomException.class,
        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
  }

  private TraceCard givenCard(CardStatus status) {
    Interview interview = Interview.builder().handoverPackage(handoverPackage).build();
    ReflectionTestUtils.setField(interview, "id", UUID.randomUUID());
    TraceCard card = TraceCard.builder()
        .handoverPackage(handoverPackage).interview(interview).title("제목").build();
    ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(card, "status", status);
    return card;
  }

  private static Member givenMember(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private static AuthenticatedMember requesterOf(Member member) {
    return new AuthenticatedMember(member.getId(), member.getUsername(), member.getRole(), member.getPosition());
  }
}
