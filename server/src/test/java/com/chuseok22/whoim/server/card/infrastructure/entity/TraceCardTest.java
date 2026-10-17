package com.chuseok22.whoim.server.card.infrastructure.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TraceCardTest {

  @Test
  void confirmByPredecessor_movesDraftToConfirmed() {
    TraceCard card = givenCard(CardStatus.DRAFT);

    card.confirmByPredecessor();

    assertThat(card.getStatus()).isEqualTo(CardStatus.PREDECESSOR_CONFIRMED);
  }

  @Test
  void approve_movesConfirmedToApproved() {
    TraceCard card = givenCard(CardStatus.PREDECESSOR_CONFIRMED);

    card.approve();

    assertThat(card.getStatus()).isEqualTo(CardStatus.APPROVED);
  }

  @Test
  void confirmByPredecessor_rejectsNonDraft() {
    for (CardStatus status : new CardStatus[] {
        CardStatus.PREDECESSOR_CONFIRMED, CardStatus.APPROVED, CardStatus.REJECTED}) {
      TraceCard card = givenCard(status);

      Throwable thrown = catchThrowable(card::confirmByPredecessor);

      assertThat(thrown).isInstanceOfSatisfying(CustomException.class,
          e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT));
      assertThat(card.getStatus()).isEqualTo(status);
    }
  }

  @Test
  void approve_rejectsNonConfirmed() {
    for (CardStatus status : new CardStatus[] {
        CardStatus.DRAFT, CardStatus.APPROVED, CardStatus.REJECTED}) {
      TraceCard card = givenCard(status);

      Throwable thrown = catchThrowable(card::approve);

      assertThat(thrown).isInstanceOfSatisfying(CustomException.class,
          e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STATE_CONFLICT));
      assertThat(card.getStatus()).isEqualTo(status);
    }
  }

  private static TraceCard givenCard(CardStatus status) {
    TraceCard card = TraceCard.builder().title("제목").build();
    ReflectionTestUtils.setField(card, "status", status);
    return card;
  }
}
