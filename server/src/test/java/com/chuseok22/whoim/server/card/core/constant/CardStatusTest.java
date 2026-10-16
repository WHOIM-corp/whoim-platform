package com.chuseok22.whoim.server.card.core.constant;

import static com.chuseok22.whoim.server.card.core.constant.CardStatus.APPROVED;
import static com.chuseok22.whoim.server.card.core.constant.CardStatus.DRAFT;
import static com.chuseok22.whoim.server.card.core.constant.CardStatus.PREDECESSOR_CONFIRMED;
import static com.chuseok22.whoim.server.card.core.constant.CardStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardStatusTest {

  @Test
  void draftCanTransitionOnlyToPredecessorConfirmed() {
    assertThat(DRAFT.canTransitionTo(PREDECESSOR_CONFIRMED)).isTrue();
    assertThat(DRAFT.canTransitionTo(APPROVED)).isFalse();
    assertThat(DRAFT.canTransitionTo(REJECTED)).isFalse();
    assertThat(DRAFT.canTransitionTo(DRAFT)).isFalse();
  }

  @Test
  void predecessorConfirmedCanBeApprovedOrRejected() {
    assertThat(PREDECESSOR_CONFIRMED.canTransitionTo(APPROVED)).isTrue();
    assertThat(PREDECESSOR_CONFIRMED.canTransitionTo(REJECTED)).isTrue();
    assertThat(PREDECESSOR_CONFIRMED.canTransitionTo(DRAFT)).isFalse();
    assertThat(PREDECESSOR_CONFIRMED.canTransitionTo(PREDECESSOR_CONFIRMED)).isFalse();
  }

  @Test
  void approvedAndRejectedAreTerminal() {
    for (CardStatus terminal : new CardStatus[]{APPROVED, REJECTED}) {
      for (CardStatus next : CardStatus.values()) {
        assertThat(terminal.canTransitionTo(next)).isFalse();
      }
    }
  }

  @Test
  void nullTargetIsRejected() {
    for (CardStatus status : CardStatus.values()) {
      assertThat(status.canTransitionTo(null)).isFalse();
    }
  }
}
