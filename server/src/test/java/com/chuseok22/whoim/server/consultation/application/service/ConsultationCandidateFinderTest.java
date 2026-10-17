package com.chuseok22.whoim.server.consultation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsultationCandidateFinderTest {

  @Mock
  private TraceCardRepository traceCardRepository;

  private ConsultationCandidateFinder finder;

  private Member predecessor;
  private Member successor;

  @BeforeEach
  void setUp() {
    finder = new ConsultationCandidateFinder(traceCardRepository);
    predecessor = givenMember("predecessor", Position.PREDECESSOR);
    successor = givenMember("successor", Position.SUCCESSOR);
  }

  @Test
  void find_queriesApprovedCardsOfPublishedPackagesForTheSuccessor() {
    given(traceCardRepository.findAllForSuccessor(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successor.getId()))
        .willReturn(List.of());

    finder.find(successor.getId());

    verify(traceCardRepository)
        .findAllForSuccessor(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successor.getId());
  }

  @Test
  void find_returnsOnlyApprovedCardsOfPublishedPackagesAssignedToTheSuccessor() {
    TraceCard visible = givenCard(CardStatus.APPROVED, givenPackage(HandoverStatus.PUBLISHED, successor));
    List<TraceCard> leaked = List.of(
        givenCard(CardStatus.DRAFT, givenPackage(HandoverStatus.PUBLISHED, successor)),
        givenCard(CardStatus.PREDECESSOR_CONFIRMED, givenPackage(HandoverStatus.PUBLISHED, successor)),
        givenCard(CardStatus.REJECTED, givenPackage(HandoverStatus.PUBLISHED, successor)),
        givenCard(CardStatus.APPROVED, givenPackage(HandoverStatus.WRITING, successor)),
        givenCard(CardStatus.APPROVED, givenPackage(HandoverStatus.PUBLISHED, givenMember("other", Position.SUCCESSOR))),
        givenCard(CardStatus.APPROVED, givenPackage(HandoverStatus.PUBLISHED, null)));
    List<TraceCard> repositoryResult = new ArrayList<>(leaked);
    repositoryResult.add(visible);
    given(traceCardRepository.findAllForSuccessor(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successor.getId()))
        .willReturn(repositoryResult);

    List<TraceCard> found = finder.find(successor.getId());

    assertThat(found).containsExactly(visible);
  }

  @Test
  void find_limitsCandidatesToTheMaximum() {
    HandoverPackage published = givenPackage(HandoverStatus.PUBLISHED, successor);
    List<TraceCard> cards = new ArrayList<>();
    for (int count = 0; count < ConsultationCandidateFinder.MAX_CANDIDATES + 2; count++) {
      cards.add(givenCard(CardStatus.APPROVED, published));
    }
    given(traceCardRepository.findAllForSuccessor(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successor.getId()))
        .willReturn(cards);

    List<TraceCard> found = finder.find(successor.getId());

    assertThat(found).hasSize(ConsultationCandidateFinder.MAX_CANDIDATES);
    assertThat(found).containsExactlyElementsOf(cards.subList(0, ConsultationCandidateFinder.MAX_CANDIDATES));
  }

  private HandoverPackage givenPackage(HandoverStatus status, Member assignedSuccessor) {
    HandoverPackage handoverPackage = HandoverPackage.builder()
        .predecessor(predecessor).successor(assignedSuccessor).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(handoverPackage, "status", status);
    return handoverPackage;
  }

  private static TraceCard givenCard(CardStatus status, HandoverPackage handoverPackage) {
    TraceCard card = TraceCard.builder().handoverPackage(handoverPackage).title("제목").build();
    ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(card, "status", status);
    return card;
  }

  private static Member givenMember(String name, Position position) {
    Member member = Member.builder().username(name).name(name).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }
}
