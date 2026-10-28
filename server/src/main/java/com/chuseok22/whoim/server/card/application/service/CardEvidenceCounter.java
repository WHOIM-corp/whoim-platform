package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.EvidenceCountRow;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 목록 응답에서 Card마다 쿼리하지 않도록 근거 개수를 한 번에 센다
@Component
@RequiredArgsConstructor
public class CardEvidenceCounter {

  private final CardEvidenceLinkRepository cardEvidenceLinkRepository;

  public Map<UUID, EvidenceCounts> countFor(Collection<TraceCard> cards) {
    if (cards.isEmpty()) {
      return Map.of();
    }
    List<UUID> cardIds = cards.stream().map(TraceCard::getId).toList();
    Map<UUID, int[]> totals = new HashMap<>();
    for (UUID cardId : cardIds) {
      totals.put(cardId, new int[2]);
    }
    for (EvidenceCountRow row : cardEvidenceLinkRepository.countByCardIds(cardIds)) {
      int[] total = totals.get(row.getCardId());
      if (total != null) {
        total[row.getType() == EvidenceLinkType.DIRECT ? 0 : 1] = row.getTotal().intValue();
      }
    }
    Map<UUID, EvidenceCounts> counts = new HashMap<>();
    totals.forEach((cardId, total) -> counts.put(cardId, new EvidenceCounts(total[0], total[1])));
    return counts;
  }
}
