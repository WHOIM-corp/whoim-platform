package com.chuseok22.whoim.server.handover.core.constant;

public enum HandoverStatus {
  WRITING,
  PUBLISHED;

  public boolean canTransitionTo(HandoverStatus next) {
    return switch (this) {
      case WRITING -> next == PUBLISHED;
      case PUBLISHED -> false;
    };
  }
}
