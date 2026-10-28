package com.chuseok22.whoim.server.handover.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.card.application.service.CardEvidenceCounter;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.EvidenceCountRow;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageCreateRequest;
import com.chuseok22.whoim.server.handover.application.dto.request.HandoverPackageSuccessorRequest;
import com.chuseok22.whoim.server.handover.application.dto.response.HandoverPackageResponse;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import com.chuseok22.whoim.server.member.infrastructure.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class HandoverPackageServiceTest {

  @Mock
  private HandoverPackageRepository handoverPackageRepository;
  @Mock
  private MemberRepository memberRepository;
  @Mock
  private TraceCardRepository traceCardRepository;
  @Mock
  private CardEvidenceLinkRepository cardEvidenceLinkRepository;

  private static final Instant NOW = Instant.parse("2026-10-17T00:00:00Z");

  private HandoverPackageService service;

  private Member predecessor;
  private Member successor;
  private Member reviewer;

  @BeforeEach
  void setUp() {
    service = new HandoverPackageService(
        handoverPackageRepository, memberRepository, traceCardRepository,
        new CardEvidenceCounter(cardEvidenceLinkRepository), Clock.fixed(NOW, ZoneOffset.UTC));
    predecessor = givenMember("predecessor", Position.PREDECESSOR);
    successor = givenMember("successor", Position.SUCCESSOR);
    reviewer = givenMember("reviewer", Position.REVIEWER);
  }

  @Test
  void create_savesWritingPackage() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(memberRepository.findById(successor.getId())).willReturn(Optional.of(successor));
    given(handoverPackageRepository.save(any(HandoverPackage.class))).willAnswer(returnsFirstArg());

    HandoverPackageResponse response = service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), successor.getId(), "인수인계", "설명"));

    ArgumentCaptor<HandoverPackage> captor = ArgumentCaptor.forClass(HandoverPackage.class);
    verify(handoverPackageRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(HandoverStatus.WRITING);
    assertThat(captor.getValue().getSuccessor()).isSameAs(successor);
    assertThat(response.name()).isEqualTo("인수인계");
    assertThat(response.description()).isEqualTo("설명");
    assertThat(response.status()).isEqualTo(HandoverStatus.WRITING);
    assertThat(response.predecessor().memberId()).isEqualTo(predecessor.getId());
    assertThat(response.successor().memberId()).isEqualTo(successor.getId());
  }

  @Test
  void create_withoutSuccessor_savesNullSuccessor() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(handoverPackageRepository.save(any(HandoverPackage.class))).willAnswer(returnsFirstArg());

    HandoverPackageResponse response = service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), null, "인수인계", null));

    assertThat(response.successor()).isNull();
    assertThat(response.description()).isNull();
  }

  @Test
  void create_rejectsPredecessorWithWrongPosition() {
    given(memberRepository.findById(successor.getId())).willReturn(Optional.of(successor));

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(successor.getId(), null, "인수인계", null))))
        .isEqualTo(ErrorCode.INVALID_REQUEST);
  }

  @Test
  void create_rejectsSuccessorWithWrongPosition() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), reviewer.getId(), "인수인계", null))))
        .isEqualTo(ErrorCode.INVALID_REQUEST);
  }

  @Test
  void create_rejectsDuplicateName() {
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));
    given(handoverPackageRepository.existsByPredecessorAndName(predecessor, "인수인계")).willReturn(true);

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(predecessor.getId(), null, "인수인계", null))))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  void create_unknownPredecessor() {
    UUID unknownId = UUID.randomUUID();
    given(memberRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.create(
        new HandoverPackageCreateRequest(unknownId, null, "인수인계", null))))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void updateSuccessor_assignsAndClears() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(memberRepository.findById(successor.getId())).willReturn(Optional.of(successor));

    HandoverPackageResponse assigned = service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(successor.getId()));
    assertThat(assigned.successor().memberId()).isEqualTo(successor.getId());

    HandoverPackageResponse cleared = service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(null));
    assertThat(cleared.successor()).isNull();
  }

  @Test
  void updateSuccessor_rejectsWrongPosition() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    assertThat(errorCodeOf(() -> service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(reviewer.getId()))))
        .isEqualTo(ErrorCode.INVALID_REQUEST);
  }

  @Test
  void updateSuccessor_rejectsPublished() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    given(handoverPackageRepository.findWithLockById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));

    assertThat(errorCodeOf(() -> service.updateSuccessor(
        handoverPackage.getId(), new HandoverPackageSuccessorRequest(null))))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  void updateSuccessor_unknownPackage() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findWithLockById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.updateSuccessor(unknownId, new HandoverPackageSuccessorRequest(null))))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void list_reviewerSeesAll_predecessorSeesOwnOnly() {
    HandoverPackage owned = givenPackage(predecessor);
    HandoverPackage other = givenPackage(givenMember("other", Position.PREDECESSOR));
    given(handoverPackageRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(owned, other));
    given(handoverPackageRepository.findAllByPredecessorOrderByCreatedAtDesc(predecessor)).willReturn(List.of(owned));
    given(memberRepository.findById(predecessor.getId())).willReturn(Optional.of(predecessor));

    assertThat(service.list(requesterOf(reviewer))).hasSize(2);
    assertThat(service.list(requesterOf(predecessor)))
        .extracting(HandoverPackageResponse::id).containsExactly(owned.getId());
  }

  @Test
  void list_rejectsOtherPositions() {
    Member admin = Member.builder().username("admin").name("admin").role(Role.ADMIN).build();
    ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

    assertThat(errorCodeOf(() -> service.list(requesterOf(admin)))).isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void list_asSuccessor_returnsOnlyOwnPublished_withApprovedCardsOnly() {
    HandoverPackage published = givenPackageWithSuccessor();
    ReflectionTestUtils.setField(published, "status", HandoverStatus.PUBLISHED);
    TraceCard approved = givenCard(published, CardStatus.APPROVED);
    TraceCard draft = givenCard(published, CardStatus.DRAFT);
    TraceCard rejected = givenCard(published, CardStatus.REJECTED);
    given(handoverPackageRepository.findAllBySuccessorIdAndStatusOrderByCreatedAtDesc(
        successor.getId(), HandoverStatus.PUBLISHED)).willReturn(List.of(published));
    given(traceCardRepository.findAllByHandoverPackageIn(List.of(published)))
        .willReturn(List.of(approved, draft, rejected));

    List<HandoverPackageResponse> responses = service.list(requesterOf(successor));

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).cards()).extracting(card -> card.id()).containsExactly(approved.getId());
    assertThat(responses.get(0).progress().activeCount()).isEqualTo(1);
    assertThat(responses.get(0).progress().approvedCount()).isEqualTo(1);
  }

  @Test
  void get_asSuccessor_ownPublished_returnsApprovedCardsOnly() {
    HandoverPackage published = givenPackageWithSuccessor();
    ReflectionTestUtils.setField(published, "status", HandoverStatus.PUBLISHED);
    TraceCard approved = givenCard(published, CardStatus.APPROVED);
    TraceCard draft = givenCard(published, CardStatus.DRAFT);
    given(handoverPackageRepository.findById(published.getId())).willReturn(Optional.of(published));
    given(traceCardRepository.findAllByHandoverPackage(published)).willReturn(List.of(approved, draft));

    HandoverPackageResponse response = service.get(requesterOf(successor), published.getId());

    assertThat(response.cards()).extracting(card -> card.id()).containsExactly(approved.getId());
    assertThat(response.progress().activeCount()).isEqualTo(1);
  }

  @Test
  void get_asSuccessor_writingOrOtherSuccessor_throwsAccessDenied() {
    HandoverPackage writing = givenPackageWithSuccessor();
    HandoverPackage othersPublished = givenPackage(predecessor);
    othersPublished.assignSuccessor(givenMember("other", Position.SUCCESSOR));
    ReflectionTestUtils.setField(othersPublished, "status", HandoverStatus.PUBLISHED);
    given(handoverPackageRepository.findById(writing.getId())).willReturn(Optional.of(writing));
    given(handoverPackageRepository.findById(othersPublished.getId())).willReturn(Optional.of(othersPublished));

    assertThat(errorCodeOf(() -> service.get(requesterOf(successor), writing.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
    assertThat(errorCodeOf(() -> service.get(requesterOf(successor), othersPublished.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void get_allowsReviewerAndOwnerOnly() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));

    assertThat(service.get(requesterOf(reviewer), handoverPackage.getId()).id()).isEqualTo(handoverPackage.getId());
    assertThat(service.get(requesterOf(predecessor), handoverPackage.getId()).id()).isEqualTo(handoverPackage.getId());
  }

  @Test
  void get_rejectsOtherPredecessor() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    AuthenticatedMember otherPredecessor = requesterOf(givenMember("other", Position.PREDECESSOR));

    assertThat(errorCodeOf(() -> service.get(otherPredecessor, handoverPackage.getId())))
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void get_unknownId() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.get(requesterOf(reviewer), unknownId)))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void list_includesProgressGroupedByPackage_withSingleCardQuery() {
    HandoverPackage first = givenPackage(predecessor);
    HandoverPackage second = givenPackage(predecessor);
    given(handoverPackageRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(first, second));
    given(traceCardRepository.findAllByHandoverPackageIn(List.of(first, second))).willReturn(List.of(
        givenCard(first, CardStatus.APPROVED), givenCard(first, CardStatus.DRAFT),
        givenCard(first, CardStatus.REJECTED)));

    List<HandoverPackageResponse> responses = service.list(requesterOf(reviewer));

    assertThat(responses.get(0).progress().approvedCount()).isEqualTo(1);
    assertThat(responses.get(0).progress().activeCount()).isEqualTo(2);
    assertThat(responses.get(0).progress().ratio()).isEqualTo(0.5);
    assertThat(responses.get(0).cards()).hasSize(3);
    assertThat(responses.get(1).progress().activeCount()).isZero();
    assertThat(responses.get(1).progress().ratio()).isZero();
    assertThat(responses.get(1).cards()).isEmpty();
  }

  @Test
  void list_emptyPackages_skipsCardQuery() {
    given(handoverPackageRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of());

    assertThat(service.list(requesterOf(reviewer))).isEmpty();

    verifyNoInteractions(traceCardRepository);
  }

  @Test
  void get_includesProgressAndCards() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(traceCardRepository.findAllByHandoverPackage(handoverPackage)).willReturn(List.of(
        givenCard(handoverPackage, CardStatus.APPROVED), givenCard(handoverPackage, CardStatus.APPROVED),
        givenCard(handoverPackage, CardStatus.PREDECESSOR_CONFIRMED)));

    HandoverPackageResponse response = service.get(requesterOf(reviewer), handoverPackage.getId());

    assertThat(response.progress().approvedCount()).isEqualTo(2);
    assertThat(response.progress().activeCount()).isEqualTo(3);
    assertThat(response.cards()).hasSize(3);
    assertThat(response.publishedAt()).isNull();
  }

  @Test
  void get_cardsIncludeActionAndEvidenceCounts() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    TraceCard card = givenCard(handoverPackage, CardStatus.APPROVED);
    ReflectionTestUtils.setField(card, "action", "재시도 5회 유지");
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(traceCardRepository.findAllByHandoverPackage(handoverPackage)).willReturn(List.of(card));
    given(cardEvidenceLinkRepository.countByCardIds(List.of(card.getId()))).willReturn(List.of(
        row(card, EvidenceLinkType.DIRECT, 2L), row(card, EvidenceLinkType.RECORDED, 3L)));

    HandoverPackageResponse response = service.get(requesterOf(reviewer), handoverPackage.getId());

    assertThat(response.cards().get(0).action()).isEqualTo("재시도 5회 유지");
    assertThat(response.cards().get(0).directEvidenceCount()).isEqualTo(2);
    assertThat(response.cards().get(0).recordedEvidenceCount()).isEqualTo(3);
  }

  @Test
  void list_countsAreLoadedOncePerRequest() {
    HandoverPackage first = givenPackage(predecessor);
    HandoverPackage second = givenPackage(predecessor);
    TraceCard firstCard = givenCard(first, CardStatus.APPROVED);
    TraceCard secondCard = givenCard(second, CardStatus.DRAFT);
    given(handoverPackageRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(first, second));
    given(traceCardRepository.findAllByHandoverPackageIn(List.of(first, second)))
        .willReturn(List.of(firstCard, secondCard));
    given(cardEvidenceLinkRepository.countByCardIds(anyCollection()))
        .willReturn(List.of(row(secondCard, EvidenceLinkType.RECORDED, 1L)));

    List<HandoverPackageResponse> responses = service.list(requesterOf(reviewer));

    verify(cardEvidenceLinkRepository, times(1)).countByCardIds(anyCollection());
    assertThat(responses.get(0).cards().get(0).recordedEvidenceCount()).isZero();
    assertThat(responses.get(1).cards().get(0).recordedEvidenceCount()).isEqualTo(1);
  }

  @Test
  void publish_success() {
    HandoverPackage handoverPackage = givenPackageWithSuccessor();
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(traceCardRepository.findAllByHandoverPackage(handoverPackage)).willReturn(List.of(
        givenCard(handoverPackage, CardStatus.APPROVED), givenCard(handoverPackage, CardStatus.REJECTED)));
    given(memberRepository.findById(reviewer.getId())).willReturn(Optional.of(reviewer));

    HandoverPackageResponse response = service.publish(requesterOf(reviewer), handoverPackage.getId());

    assertThat(response.status()).isEqualTo(HandoverStatus.PUBLISHED);
    assertThat(response.publishedAt().toInstant()).isEqualTo(NOW);
    assertThat(handoverPackage.getPublishedBy()).isSameAs(reviewer);
    assertThat(handoverPackage.getPublishedAt()).isEqualTo(NOW);
  }

  @Test
  void publish_rejectsPackageWithoutSuccessor() {
    HandoverPackage handoverPackage = givenPackage(predecessor);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));

    assertThat(errorCodeOf(() -> service.publish(requesterOf(reviewer), handoverPackage.getId())))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
    assertThat(handoverPackage.getStatus()).isEqualTo(HandoverStatus.WRITING);
  }

  @Test
  void publish_rejectsUnapprovedCard() {
    HandoverPackage handoverPackage = givenPackageWithSuccessor();
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(traceCardRepository.findAllByHandoverPackage(handoverPackage)).willReturn(List.of(
        givenCard(handoverPackage, CardStatus.APPROVED), givenCard(handoverPackage, CardStatus.PREDECESSOR_CONFIRMED)));

    assertThat(errorCodeOf(() -> service.publish(requesterOf(reviewer), handoverPackage.getId())))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
    assertThat(handoverPackage.getStatus()).isEqualTo(HandoverStatus.WRITING);
  }

  @Test
  void publish_rejectsZeroActiveCards() {
    HandoverPackage handoverPackage = givenPackageWithSuccessor();
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));
    given(traceCardRepository.findAllByHandoverPackage(handoverPackage))
        .willReturn(List.of(givenCard(handoverPackage, CardStatus.REJECTED)));

    assertThat(errorCodeOf(() -> service.publish(requesterOf(reviewer), handoverPackage.getId())))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
    assertThat(handoverPackage.getStatus()).isEqualTo(HandoverStatus.WRITING);
  }

  @Test
  void publish_rejectsAlreadyPublished() {
    HandoverPackage handoverPackage = givenPackageWithSuccessor();
    ReflectionTestUtils.setField(handoverPackage, "status", HandoverStatus.PUBLISHED);
    given(handoverPackageRepository.findById(handoverPackage.getId())).willReturn(Optional.of(handoverPackage));

    assertThat(errorCodeOf(() -> service.publish(requesterOf(reviewer), handoverPackage.getId())))
        .isEqualTo(ErrorCode.STATE_CONFLICT);
    verify(traceCardRepository, never()).findAllByHandoverPackage(any());
  }

  @Test
  void publish_unknownPackage() {
    UUID unknownId = UUID.randomUUID();
    given(handoverPackageRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThat(errorCodeOf(() -> service.publish(requesterOf(reviewer), unknownId)))
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void handoverPackagePublish_setsFieldsAndRejectsSecondPublish() {
    HandoverPackage handoverPackage = givenPackageWithSuccessor();

    handoverPackage.publish(reviewer, NOW);

    assertThat(handoverPackage.getStatus()).isEqualTo(HandoverStatus.PUBLISHED);
    assertThat(handoverPackage.getPublishedAt()).isEqualTo(NOW);
    assertThat(handoverPackage.getPublishedBy()).isSameAs(reviewer);
    assertThat(errorCodeOf(() -> handoverPackage.publish(reviewer, NOW))).isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  private HandoverPackage givenPackageWithSuccessor() {
    HandoverPackage created = givenPackage(predecessor);
    created.assignSuccessor(successor);
    return created;
  }

  private static EvidenceCountRow row(TraceCard card, EvidenceLinkType type, long total) {
    return new FixedEvidenceCountRow(card.getId(), type, total);
  }

  private record FixedEvidenceCountRow(UUID cardId, EvidenceLinkType type, Long total) implements EvidenceCountRow {

    @Override
    public UUID getCardId() {
      return cardId;
    }

    @Override
    public EvidenceLinkType getType() {
      return type;
    }

    @Override
    public Long getTotal() {
      return total;
    }
  }

  private static TraceCard givenCard(HandoverPackage handoverPackage, CardStatus status) {
    TraceCard card = TraceCard.builder().handoverPackage(handoverPackage).title("card").build();
    ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(card, "status", status);
    return card;
  }

  private static ErrorCode errorCodeOf(Runnable action) {
    Throwable thrown = catchThrowable(action::run);
    assertThat(thrown).isInstanceOf(CustomException.class);
    return ((CustomException) thrown).getErrorCode();
  }

  private static Member givenMember(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }

  private static HandoverPackage givenPackage(Member owner) {
    HandoverPackage created = HandoverPackage.builder().predecessor(owner).name("package").build();
    ReflectionTestUtils.setField(created, "id", UUID.randomUUID());
    return created;
  }

  private static AuthenticatedMember requesterOf(Member member) {
    return new AuthenticatedMember(member.getId(), member.getUsername(), member.getRole(), member.getPosition());
  }
}
