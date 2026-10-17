package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminPaging;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.infrastructure.entity.Member;
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
public class HandoverPackageTableView implements AdminTableView {

  private static final List<String> COLUMNS = List.of(
      "id", "name", "status", "predecessor_id", "successor_id", "published_at", "created_at");

  private final HandoverPackageRepository handoverPackageRepository;

  @Override
  public AdminTable table() {
    return AdminTable.HANDOVER_PACKAGE;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return handoverPackageRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Instant lastCreatedAt() {
    return handoverPackageRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream().findFirst().map(HandoverPackage::getCreatedAt).orElse(null);
  }

  // 패키지 필터는 없으므로 handoverPackageId는 무시한다
  @Override
  @Transactional(readOnly = true)
  public AdminListResponse list(int page, UUID handoverPackageId) {
    Page<HandoverPackage> packages = handoverPackageRepository.findAll(AdminPaging.pageRequest(page));
    List<AdminRowResponse> rows = packages.getContent().stream()
        .map(handoverPackage -> new AdminRowResponse(handoverPackage.getId(), List.of(
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, handoverPackage.getId()),
            AdminCell.truncated(handoverPackage.getName()),
            AdminCell.badge(handoverPackage.getStatus().name()),
            memberLink(handoverPackage.getPredecessor()),
            memberLink(handoverPackage.getSuccessor()),
            AdminCell.time(handoverPackage.getPublishedAt()),
            AdminCell.time(handoverPackage.getCreatedAt()))))
        .toList();
    return new AdminListResponse(AdminTable.HANDOVER_PACKAGE.pathName(), COLUMNS, rows, packages.getNumber(),
        packages.getTotalPages(), packages.getTotalElements(), null, List.of());
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDetailResponse detail(UUID id) {
    HandoverPackage handoverPackage = handoverPackageRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    return new AdminDetailResponse(AdminTable.HANDOVER_PACKAGE.pathName(), handoverPackage.getId(), List.of(
        new AdminFieldResponse("id", AdminCell.text(handoverPackage.getId().toString())),
        new AdminFieldResponse("name", AdminCell.text(handoverPackage.getName())),
        new AdminFieldResponse("description", AdminCell.text(handoverPackage.getDescription())),
        new AdminFieldResponse("status", AdminCell.badge(handoverPackage.getStatus().name())),
        new AdminFieldResponse("predecessor_id", memberLink(handoverPackage.getPredecessor())),
        new AdminFieldResponse("successor_id", memberLink(handoverPackage.getSuccessor())),
        new AdminFieldResponse("published_at", AdminCell.time(handoverPackage.getPublishedAt())),
        new AdminFieldResponse("published_by_id", memberLink(handoverPackage.getPublishedBy())),
        new AdminFieldResponse("created_at", AdminCell.time(handoverPackage.getCreatedAt())),
        new AdminFieldResponse("updated_at", AdminCell.time(handoverPackage.getUpdatedAt()))), List.of());
  }

  private AdminCell memberLink(Member member) {
    return member == null ? AdminCell.blank() : AdminCell.link(AdminTable.MEMBER, member.getId());
  }
}
