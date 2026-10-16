package com.chuseok22.whoim.server.card.core.constant;

public enum CardStatus {
  DRAFT,
  PREDECESSOR_CONFIRMED,
  APPROVED,
  REJECTED;

  public boolean canTransitionTo(CardStatus next) {
    return switch (this) {
      case DRAFT -> next == PREDECESSOR_CONFIRMED;
      case PREDECESSOR_CONFIRMED -> next == APPROVED || next == REJECTED;
      case APPROVED, REJECTED -> false;
    };
  }
}
