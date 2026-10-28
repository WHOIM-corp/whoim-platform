package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import java.util.List;
import org.springframework.stereotype.Component;

// 발언 근거(DIRECT)가 뒷받침하는 TRACE 요소를 인터뷰 상태에서 찾는다. 문서 근거(RECORDED)는 Gap 단위라 요소와 연결하지 않는다
@Component
public class EvidenceElementResolver {

  public List<TraceElement> resolve(TraceState state, CardEvidenceLink link) {
    if (link.getType() != EvidenceLinkType.DIRECT || link.getTurn() == null) {
      return List.of();
    }
    int turnNumber = link.getTurn().getTurnNumber();
    return List.of(TraceElement.values()).stream()
        .filter(element -> isFilledDirectlyAtTurn(state.entries().get(element), turnNumber))
        .toList();
  }

  private boolean isFilledDirectlyAtTurn(TraceStateEntry entry, int turnNumber) {
    return entry != null
        && entry.status() == TraceElementStatus.FILLED
        && entry.source() == EvidenceLinkType.DIRECT
        && entry.turnNumber() == turnNumber;
  }
}
