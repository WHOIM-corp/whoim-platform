package com.chuseok22.whoim.server.admin.application.service.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.card.core.constant.CardStatus;
import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TraceCardTableViewTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-18T00:00:00Z");

  @Mock
  private TraceCardRepository traceCardRepository;

  @Mock
  private CardEvidenceLinkRepository cardEvidenceLinkRepository;

  @Mock
  private CardStateHistoryRepository cardStateHistoryRepository;

  @Mock
  private AdminPackageOptions adminPackageOptions;

  @InjectMocks
  private TraceCardTableView traceCardTableView;

  @Test
  void list_linksInterview() {
    TraceCard card = card();
    given(traceCardRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(card)));

    AdminListResponse response = traceCardTableView.list(0, null);

    assertThat(response.columns())
        .containsExactly("id", "handover_package_id", "interview_id", "title", "status", "created_at");
    assertThat(response.rows().get(0).cells().get(2).linkTableName()).isEqualTo("interview");
  }

  @Test
  void list_withPackageId_usesPackageQuery() {
    UUID packageId = UUID.randomUUID();
    given(traceCardRepository.findAllByHandoverPackageId(eq(packageId), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    traceCardTableView.list(0, packageId);

    verify(traceCardRepository).findAllByHandoverPackageId(eq(packageId), any(Pageable.class));
  }

  @Test
  void detail_showsTraceFieldsEvenWhenNull() {
    TraceCard card = card();
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of());
    given(cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of());

    AdminDetailResponse response = traceCardTableView.detail(card.getId());

    var labels = response.fields().stream().map(field -> field.label()).toList();
    assertThat(labels).contains("situation", "signal", "cause", "condition", "judgment",
        "alternative", "action", "reason", "exception", "risk");
    var situation = response.fields().stream().filter(field -> field.label().equals("situation")).findFirst().orElseThrow();
    assertThat(situation.value().text()).isEqualTo("상황");
    var risk = response.fields().stream().filter(field -> field.label().equals("risk")).findFirst().orElseThrow();
    assertThat(risk.value().empty()).isTrue();
  }

  @Test
  void detail_showsEvidenceLinksAndHistoryWithNullableCells() {
    TraceCard card = card();
    InterviewTurn turn = InterviewTurn.builder().interview(card.getInterview()).turnNumber(1).question("q").build();
    ReflectionTestUtils.setField(turn, "id", UUID.randomUUID());
    EvidenceDocument document = EvidenceDocument.builder().build();
    UUID documentId = UUID.randomUUID();
    ReflectionTestUtils.setField(document, "id", documentId);
    CardEvidenceLink direct = link(card, EvidenceLinkType.DIRECT, turn, null, null, null);
    CardEvidenceLink recorded = link(card, EvidenceLinkType.RECORDED, null, document, 3, 9);
    Member actor = Member.builder().username("reviewer").build();
    UUID actorId = UUID.randomUUID();
    ReflectionTestUtils.setField(actor, "id", actorId);
    CardStateHistory created = history(card, null, CardStatus.DRAFT, null);
    CardStateHistory approved = history(card, CardStatus.DRAFT, CardStatus.APPROVED, actor);
    given(traceCardRepository.findById(card.getId())).willReturn(Optional.of(card));
    given(cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of(direct, recorded));
    given(cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)).willReturn(List.of(created, approved));

    AdminDetailResponse response = traceCardTableView.detail(card.getId());

    assertThat(response.sections()).hasSize(2);
    AdminSectionResponse links = response.sections().get(0);
    assertThat(links.title()).isEqualTo("근거 링크 (card_evidence_link)");
    assertThat(links.kind()).isEqualTo("TABLE");
    assertThat(links.columns()).containsExactly(
        "id", "type", "turn_id", "document_id", "start_offset", "end_offset", "excerpt", "created_at");
    var directCells = links.rows().get(0).cells();
    assertThat(directCells.get(2).hasLink()).isFalse();
    assertThat(directCells.get(2).empty()).isFalse();
    assertThat(directCells.get(3).empty()).isTrue();
    assertThat(directCells.get(4).empty()).isTrue();
    assertThat(directCells.get(5).empty()).isTrue();
    var recordedCells = links.rows().get(1).cells();
    assertThat(recordedCells.get(2).empty()).isTrue();
    assertThat(recordedCells.get(3).linkTableName()).isEqualTo("evidence_document");
    assertThat(recordedCells.get(3).linkId()).isEqualTo(documentId);
    assertThat(recordedCells.get(4).text()).isEqualTo("3");

    AdminSectionResponse history = response.sections().get(1);
    assertThat(history.title()).isEqualTo("상태 이력 (card_state_history)");
    assertThat(history.columns()).containsExactly("from_status", "to_status", "actor_id", "reason", "created_at");
    assertThat(history.rows().get(0).cells().get(0).empty()).isTrue();
    assertThat(history.rows().get(0).cells().get(2).empty()).isTrue();
    assertThat(history.rows().get(1).cells().get(2).linkTableName()).isEqualTo("member");
    assertThat(history.rows().get(1).cells().get(2).linkId()).isEqualTo(actorId);
  }

  private TraceCard card() {
    HandoverPackage handoverPackage = HandoverPackage.builder().name("패키지").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    Interview interview = Interview.builder().handoverPackage(handoverPackage).build();
    ReflectionTestUtils.setField(interview, "id", UUID.randomUUID());
    TraceCard card = TraceCard.builder().handoverPackage(handoverPackage).interview(interview)
        .title("제목").situation("상황").build();
    ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(card, "createdAt", CREATED_AT);
    ReflectionTestUtils.setField(card, "updatedAt", CREATED_AT);
    return card;
  }

  private CardEvidenceLink link(TraceCard card, EvidenceLinkType type, InterviewTurn turn,
      EvidenceDocument document, Integer startOffset, Integer endOffset) {
    CardEvidenceLink link = CardEvidenceLink.builder().card(card).type(type).turn(turn).document(document)
        .startOffset(startOffset).endOffset(endOffset).excerpt("발췌").build();
    ReflectionTestUtils.setField(link, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(link, "createdAt", CREATED_AT);
    return link;
  }

  private CardStateHistory history(TraceCard card, CardStatus from, CardStatus to, Member actor) {
    CardStateHistory history = CardStateHistory.builder().card(card).fromStatus(from).toStatus(to).actor(actor).build();
    ReflectionTestUtils.setField(history, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(history, "createdAt", CREATED_AT);
    return history;
  }
}
