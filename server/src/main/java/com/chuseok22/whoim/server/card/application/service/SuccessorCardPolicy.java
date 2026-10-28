package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import java.util.UUID;
import org.springframework.stereotype.Component;

// 후임자에게 보여도 되는 Card의 규칙을 한 곳에 둔다. 쿼리가 틀려도 비공개 Card가 새지 않게 하는 안전망이다
@Component
public class SuccessorCardPolicy {

  public boolean isVisible(TraceCard card, UUID successorId) {
    HandoverPackage handoverPackage = card.getHandoverPackage();
    return card.getStatus() == CardStatus.APPROVED
        && handoverPackage.getStatus() == HandoverStatus.PUBLISHED
        && handoverPackage.getSuccessor() != null
        && successorId.equals(handoverPackage.getSuccessor().getId());
  }
}
