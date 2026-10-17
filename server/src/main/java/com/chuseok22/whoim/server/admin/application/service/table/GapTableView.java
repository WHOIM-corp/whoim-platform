package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminPaging;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import java.time.Instant;
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
public class GapTableView implements AdminTableView {

  private static final List<String> COLUMNS = List.of(
      "id", "handover_package_id", "type", "summary", "question", "created_at");
  private static final List<String> ANCHOR_COLUMNS = List.of(
      "id", "document_id", "start_offset", "end_offset", "excerpt", "created_at");
  private static final String TABLE_KIND = "TABLE";

  private final GapRepository gapRepository;
  private final GapAnchorRepository gapAnchorRepository;
  private final AdminPackageOptions adminPackageOptions;

  @Override
  public AdminTable table() {
    return AdminTable.GAP;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return gapRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Instant lastCreatedAt() {
    return gapRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream().findFirst().map(Gap::getCreatedAt).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminListResponse list(int page, UUID handoverPackageId) {
    Page<Gap> gaps = handoverPackageId == null
        ? gapRepository.findAll(AdminPaging.pageRequest(page))
        : gapRepository.findAllByHandoverPackageId(handoverPackageId, AdminPaging.pageRequest(page));
    List<AdminRowResponse> rows = gaps.getContent().stream()
        .map(gap -> new AdminRowResponse(gap.getId(), List.of(
            AdminCell.link(AdminTable.GAP, gap.getId()),
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, gap.getHandoverPackage().getId()),
            AdminCell.badge(gap.getType().name()),
            AdminCell.truncated(gap.getSummary()),
            AdminCell.truncated(gap.getQuestion()),
            AdminCell.time(gap.getCreatedAt()))))
        .toList();
    return new AdminListResponse(AdminTable.GAP.pathName(), COLUMNS, rows, gaps.getNumber(),
        gaps.getTotalPages(), gaps.getTotalElements(), handoverPackageId,
        adminPackageOptions.optionsFor(AdminTable.GAP));
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDetailResponse detail(UUID id) {
    Gap gap = gapRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    List<AdminRowResponse> anchorRows = gapAnchorRepository.findAllByGapOrderByCreatedAtAsc(gap).stream()
        .map(this::anchorRow)
        .toList();
    return new AdminDetailResponse(AdminTable.GAP.pathName(), gap.getId(), List.of(
        new AdminFieldResponse("id", AdminCell.text(gap.getId().toString())),
        new AdminFieldResponse("handover_package_id",
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, gap.getHandoverPackage().getId())),
        new AdminFieldResponse("type", AdminCell.badge(gap.getType().name())),
        new AdminFieldResponse("summary", AdminCell.text(gap.getSummary())),
        new AdminFieldResponse("question", AdminCell.text(gap.getQuestion())),
        new AdminFieldResponse("created_at", AdminCell.time(gap.getCreatedAt())),
        new AdminFieldResponse("updated_at", AdminCell.time(gap.getUpdatedAt()))),
        List.of(new AdminSectionResponse("근거 앵커 (gap_anchor)", TABLE_KIND, ANCHOR_COLUMNS, anchorRows)));
  }

  private AdminRowResponse anchorRow(GapAnchor anchor) {
    return new AdminRowResponse(anchor.getId(), List.of(
        AdminCell.plainId(anchor.getId()),
        AdminCell.link(AdminTable.EVIDENCE_DOCUMENT, anchor.getDocument().getId()),
        AdminCell.text(String.valueOf(anchor.getStartOffset())),
        AdminCell.text(String.valueOf(anchor.getEndOffset())),
        AdminCell.text(anchor.getExcerpt()),
        AdminCell.time(anchor.getCreatedAt())));
  }
}
