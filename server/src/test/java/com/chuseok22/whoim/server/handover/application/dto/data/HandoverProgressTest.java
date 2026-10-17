package com.chuseok22.whoim.server.handover.application.dto.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HandoverProgressTest {

  @Test
  void noCards_isNotPublishable() {
    HandoverProgress progress = HandoverProgress.from(List.of());

    assertThat(progress.activeCount()).isZero();
    assertThat(progress.approvedCount()).isZero();
    assertThat(progress.publishable()).isFalse();
  }

  @Test
  void partiallyApproved_isNotPublishable() {
    HandoverProgress progress = HandoverProgress.from(
        cardsOf(CardStatus.APPROVED, CardStatus.APPROVED, CardStatus.PREDECESSOR_CONFIRMED));

    assertThat(progress.approvedCount()).isEqualTo(2);
    assertThat(progress.activeCount()).isEqualTo(3);
    assertThat(progress.publishable()).isFalse();
  }

  @Test
  void allApproved_isPublishable() {
    HandoverProgress progress = HandoverProgress.from(cardsOf(CardStatus.APPROVED, CardStatus.APPROVED));

    assertThat(progress.publishable()).isTrue();
  }

  @Test
  void rejectedCards_areExcludedFromActive() {
    HandoverProgress progress = HandoverProgress.from(
        cardsOf(CardStatus.APPROVED, CardStatus.REJECTED, CardStatus.REJECTED));

    assertThat(progress.approvedCount()).isEqualTo(1);
    assertThat(progress.activeCount()).isEqualTo(1);
    assertThat(progress.publishable()).isTrue();
  }

  @Test
  void onlyRejectedCards_isNotPublishable() {
    HandoverProgress progress = HandoverProgress.from(cardsOf(CardStatus.REJECTED));

    assertThat(progress.activeCount()).isZero();
    assertThat(progress.publishable()).isFalse();
  }

  private static List<TraceCard> cardsOf(CardStatus... statuses) {
    return Arrays.stream(statuses).map(status -> {
      TraceCard card = TraceCard.builder().title("card").build();
      ReflectionTestUtils.setField(card, "status", status);
      return card;
    }).toList();
  }
}
