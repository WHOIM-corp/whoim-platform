package com.chuseok22.whoim.server.card.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SuccessorCardPolicyTest {

  private final SuccessorCardPolicy policy = new SuccessorCardPolicy();

  private final Member predecessor = givenMember(Position.PREDECESSOR);
  private final Member successor = givenMember(Position.SUCCESSOR);

  @Test
  void visible_whenApprovedAndPublishedAndOwnSuccessor() {
    TraceCard card = givenCard(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successor);

    assertThat(policy.isVisible(card, successor.getId())).isTrue();
  }

  @Test
  void hidden_whenNotApproved() {
    for (CardStatus status : new CardStatus[] {
        CardStatus.DRAFT, CardStatus.PREDECESSOR_CONFIRMED, CardStatus.REJECTED}) {
      TraceCard card = givenCard(status, HandoverStatus.PUBLISHED, successor);

      assertThat(policy.isVisible(card, successor.getId())).isFalse();
    }
  }

  @Test
  void hidden_whenPackageWriting() {
    TraceCard card = givenCard(CardStatus.APPROVED, HandoverStatus.WRITING, successor);

    assertThat(policy.isVisible(card, successor.getId())).isFalse();
  }

  @Test
  void hidden_whenOtherSuccessor() {
    TraceCard card = givenCard(CardStatus.APPROVED, HandoverStatus.PUBLISHED, successor);

    assertThat(policy.isVisible(card, UUID.randomUUID())).isFalse();
  }

  @Test
  void hidden_whenNoSuccessor() {
    TraceCard card = givenCard(CardStatus.APPROVED, HandoverStatus.PUBLISHED, null);

    assertThat(policy.isVisible(card, successor.getId())).isFalse();
  }

  private TraceCard givenCard(CardStatus cardStatus, HandoverStatus packageStatus, Member packageSuccessor) {
    HandoverPackage handoverPackage = HandoverPackage.builder()
        .predecessor(predecessor).successor(packageSuccessor).name("package").build();
    ReflectionTestUtils.setField(handoverPackage, "status", packageStatus);
    TraceCard card = TraceCard.builder().handoverPackage(handoverPackage).title("card").build();
    ReflectionTestUtils.setField(card, "status", cardStatus);
    return card;
  }

  private static Member givenMember(Position position) {
    Member member = Member.builder()
        .username(position.name()).name(position.name()).role(Role.USER).position(position).build();
    ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
    return member;
  }
}
