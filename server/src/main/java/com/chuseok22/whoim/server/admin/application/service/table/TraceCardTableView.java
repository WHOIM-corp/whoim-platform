package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminPaging;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardEvidenceLink;
import com.chuseok22.whoim.server.card.infrastructure.entity.CardStateHistory;
import com.chuseok22.whoim.server.card.infrastructure.entity.TraceCard;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardEvidenceLinkRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.CardStateHistoryRepository;
import com.chuseok22.whoim.server.card.infrastructure.repository.TraceCardRepository;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TraceCardTableView implements AdminTableView {

  private static final List<String> COLUMNS = List.of(
      "id", "handover_package_id", "interview_id", "title", "status", "created_at");
  private static final List<String> EVIDENCE_LINK_COLUMNS = List.of(
      "id", "type", "turn_id", "document_id", "start_offset", "end_offset", "excerpt", "created_at");
  private static final List<String> STATE_HISTORY_COLUMNS = List.of(
      "from_status", "to_status", "actor_id", "reason", "created_at");
  private static final String TABLE_KIND = "TABLE";

  private final TraceCardRepository traceCardRepository;
  private final CardEvidenceLinkRepository cardEvidenceLinkRepository;
  private final CardStateHistoryRepository cardStateHistoryRepository;
  private final AdminPackageOptions adminPackageOptions;

  @Override
  public AdminTable table() {
    return AdminTable.TRACE_CARD;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return traceCardRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Instant lastCreatedAt() {
    return traceCardRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream().findFirst().map(TraceCard::getCreatedAt).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminListResponse list(int page, UUID handoverPackageId) {
    Page<TraceCard> cards = handoverPackageId == null
        ? traceCardRepository.findAll(AdminPaging.pageRequest(page))
        : traceCardRepository.findAllByHandoverPackageId(handoverPackageId, AdminPaging.pageRequest(page));
    List<AdminRowResponse> rows = cards.getContent().stream()
        .map(card -> new AdminRowResponse(card.getId(), List.of(
            AdminCell.link(AdminTable.TRACE_CARD, card.getId()),
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, card.getHandoverPackage().getId()),
            AdminCell.link(AdminTable.INTERVIEW, card.getInterview().getId()),
            AdminCell.truncated(card.getTitle()),
            AdminCell.badge(card.getStatus().name()),
            AdminCell.time(card.getCreatedAt()))))
        .toList();
    return new AdminListResponse(AdminTable.TRACE_CARD.pathName(), COLUMNS, rows, cards.getNumber(),
        cards.getTotalPages(), cards.getTotalElements(), handoverPackageId,
        adminPackageOptions.optionsFor(AdminTable.TRACE_CARD));
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDetailResponse detail(UUID id) {
    TraceCard card = traceCardRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    List<AdminRowResponse> evidenceLinkRows = cardEvidenceLinkRepository.findAllByCardOrderByCreatedAtAsc(card)
        .stream().map(this::evidenceLinkRow).toList();
    List<AdminRowResponse> stateHistoryRows = cardStateHistoryRepository.findAllByCardOrderByCreatedAtAsc(card)
        .stream().map(this::stateHistoryRow).toList();
    return new AdminDetailResponse(AdminTable.TRACE_CARD.pathName(), card.getId(), fields(card), List.of(
        new AdminSectionResponse("근거 링크 (card_evidence_link)", TABLE_KIND, EVIDENCE_LINK_COLUMNS, evidenceLinkRows),
        new AdminSectionResponse("상태 이력 (card_state_history)", TABLE_KIND, STATE_HISTORY_COLUMNS, stateHistoryRows)));
  }

  private List<AdminFieldResponse> fields(TraceCard card) {
    List<AdminFieldResponse> fields = new ArrayList<>();
    fields.add(new AdminFieldResponse("id", AdminCell.text(card.getId().toString())));
    fields.add(new AdminFieldResponse("handover_package_id",
        AdminCell.link(AdminTable.HANDOVER_PACKAGE, card.getHandoverPackage().getId())));
    fields.add(new AdminFieldResponse("interview_id", AdminCell.link(AdminTable.INTERVIEW, card.getInterview().getId())));
    fields.add(new AdminFieldResponse("title", AdminCell.text(card.getTitle())));
    fields.add(new AdminFieldResponse("status", AdminCell.badge(card.getStatus().name())));
    fields.add(new AdminFieldResponse("situation", AdminCell.text(card.getSituation())));
    fields.add(new AdminFieldResponse("signal", AdminCell.text(card.getSignal())));
    fields.add(new AdminFieldResponse("cause", AdminCell.text(card.getCause())));
    fields.add(new AdminFieldResponse("condition", AdminCell.text(card.getCondition())));
    fields.add(new AdminFieldResponse("judgment", AdminCell.text(card.getJudgment())));
    fields.add(new AdminFieldResponse("alternative", AdminCell.text(card.getAlternative())));
    fields.add(new AdminFieldResponse("action", AdminCell.text(card.getAction())));
    fields.add(new AdminFieldResponse("reason", AdminCell.text(card.getReason())));
    fields.add(new AdminFieldResponse("exception", AdminCell.text(card.getException())));
    fields.add(new AdminFieldResponse("risk", AdminCell.text(card.getRisk())));
    fields.add(new AdminFieldResponse("created_at", AdminCell.time(card.getCreatedAt())));
    fields.add(new AdminFieldResponse("updated_at", AdminCell.time(card.getUpdatedAt())));
    return fields;
  }

  // DIRECT 행은 document·offset이, RECORDED 행은 turn이 null이므로 빈 셀로 둔다
  private AdminRowResponse evidenceLinkRow(CardEvidenceLink link) {
    return new AdminRowResponse(link.getId(), List.of(
        AdminCell.plainId(link.getId()),
        AdminCell.badge(link.getType().name()),
        AdminCell.plainId(link.getTurn() == null ? null : link.getTurn().getId()),
        AdminCell.link(AdminTable.EVIDENCE_DOCUMENT, link.getDocument() == null ? null : link.getDocument().getId()),
        AdminCell.text(link.getStartOffset() == null ? null : String.valueOf(link.getStartOffset())),
        AdminCell.text(link.getEndOffset() == null ? null : String.valueOf(link.getEndOffset())),
        AdminCell.text(link.getExcerpt()),
        AdminCell.time(link.getCreatedAt())));
  }

  private AdminRowResponse stateHistoryRow(CardStateHistory history) {
    return new AdminRowResponse(history.getId(), List.of(
        AdminCell.badge(history.getFromStatus() == null ? null : history.getFromStatus().name()),
        AdminCell.badge(history.getToStatus().name()),
        AdminCell.link(AdminTable.MEMBER, history.getActor() == null ? null : history.getActor().getId()),
        AdminCell.text(history.getReason()),
        AdminCell.time(history.getCreatedAt())));
  }
}
