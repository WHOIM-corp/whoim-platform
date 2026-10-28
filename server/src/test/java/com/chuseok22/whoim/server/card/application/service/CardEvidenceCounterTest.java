package com.chuseok22.whoim.server.card.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chuseok22.whoim.server.card.application.dto.data.EvidenceCounts;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.EvidenceCountRow;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CardEvidenceCounterTest {

  @Mock
  private CardEvidenceLinkRepository cardEvidenceLinkRepository;

  private CardEvidenceCounter counter;

  @BeforeEach
  void setUp() {
    counter = new CardEvidenceCounter(cardEvidenceLinkRepository);
  }

  @Test
  void countFor_groupsByCardAndType() {
    TraceCard first = card();
    TraceCard second = card();
    given(cardEvidenceLinkRepository.countByCardIds(anyCollection())).willReturn(List.of(
        row(first, EvidenceLinkType.DIRECT, 2L),
        row(first, EvidenceLinkType.RECORDED, 3L),
        row(second, EvidenceLinkType.RECORDED, 1L)));

    Map<UUID, EvidenceCounts> counts = counter.countFor(List.of(first, second));

    assertThat(counts.get(first.getId())).isEqualTo(new EvidenceCounts(2, 3));
    assertThat(counts.get(second.getId())).isEqualTo(new EvidenceCounts(0, 1));
  }

  @Test
  void countFor_fillsZeroForCardsWithoutLinks() {
    TraceCard withoutLinks = card();
    given(cardEvidenceLinkRepository.countByCardIds(anyCollection())).willReturn(List.of());

    assertThat(counter.countFor(List.of(withoutLinks))).containsEntry(withoutLinks.getId(), EvidenceCounts.ZERO);
  }

  @Test
  void countFor_emptyInput_doesNotQuery() {
    assertThat(counter.countFor(List.of())).isEmpty();

    verifyNoInteractions(cardEvidenceLinkRepository);
  }

  private static TraceCard card() {
    TraceCard card = TraceCard.builder().title("제목").build();
    ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
    return card;
  }

  private static EvidenceCountRow row(TraceCard card, EvidenceLinkType type, long total) {
    return new FixedEvidenceCountRow(card.getId(), type, total);
  }

  private record FixedEvidenceCountRow(UUID cardId, EvidenceLinkType type, Long total) implements EvidenceCountRow {

    @Override
    public UUID getCardId() {
      return cardId;
    }

    @Override
    public EvidenceLinkType getType() {
      return type;
    }

    @Override
    public Long getTotal() {
      return total;
    }
  }
}
