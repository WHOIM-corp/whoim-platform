package com.chuseok22.whoim.server.handover.application.dto.data;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import java.util.List;

// 활성 Card = REJECTED를 제외한 Card
public record HandoverProgress(int approvedCount, int activeCount) {

  public static HandoverProgress from(List<TraceCard> cards) {
    int activeCount = (int) cards.stream().filter(card -> card.getStatus() != CardStatus.REJECTED).count();
    int approvedCount = (int) cards.stream().filter(card -> card.getStatus() == CardStatus.APPROVED).count();
    return new HandoverProgress(approvedCount, activeCount);
  }

  public boolean publishable() {
    return activeCount > 0 && approvedCount == activeCount;
  }
}
