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
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.evidence.infrastructure.repository.EvidenceDocumentRepository;
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
public class EvidenceDocumentTableView implements AdminTableView {

  private static final List<String> COLUMNS = List.of(
      "id", "handover_package_id", "file_name", "document_type", "content", "uploaded_by_id", "created_at");

  private final EvidenceDocumentRepository evidenceDocumentRepository;
  private final AdminPackageOptions adminPackageOptions;

  @Override
  public AdminTable table() {
    return AdminTable.EVIDENCE_DOCUMENT;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return evidenceDocumentRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Instant lastCreatedAt() {
    return evidenceDocumentRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream().findFirst().map(EvidenceDocument::getCreatedAt).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminListResponse list(int page, UUID handoverPackageId) {
    Page<EvidenceDocument> documents = handoverPackageId == null
        ? evidenceDocumentRepository.findAll(AdminPaging.pageRequest(page))
        : evidenceDocumentRepository.findAllByHandoverPackageId(handoverPackageId, AdminPaging.pageRequest(page));
    List<AdminRowResponse> rows = documents.getContent().stream()
        .map(document -> new AdminRowResponse(document.getId(), List.of(
            AdminCell.link(AdminTable.EVIDENCE_DOCUMENT, document.getId()),
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, document.getHandoverPackage().getId()),
            AdminCell.truncated(document.getFileName()),
            AdminCell.badge(document.getDocumentType().name()),
            AdminCell.truncated(document.getContent()),
            AdminCell.link(AdminTable.MEMBER, document.getUploadedBy().getId()),
            AdminCell.time(document.getCreatedAt()))))
        .toList();
    return new AdminListResponse(AdminTable.EVIDENCE_DOCUMENT.pathName(), COLUMNS, rows, documents.getNumber(),
        documents.getTotalPages(), documents.getTotalElements(), handoverPackageId,
        adminPackageOptions.optionsFor(AdminTable.EVIDENCE_DOCUMENT));
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDetailResponse detail(UUID id) {
    EvidenceDocument document = evidenceDocumentRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    return new AdminDetailResponse(AdminTable.EVIDENCE_DOCUMENT.pathName(), document.getId(), List.of(
        new AdminFieldResponse("id", AdminCell.text(document.getId().toString())),
        new AdminFieldResponse("handover_package_id",
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, document.getHandoverPackage().getId())),
        new AdminFieldResponse("file_name", AdminCell.text(document.getFileName())),
        new AdminFieldResponse("document_type", AdminCell.badge(document.getDocumentType().name())),
        new AdminFieldResponse("content", AdminCell.text(document.getContent())),
        new AdminFieldResponse("uploaded_by_id", AdminCell.link(AdminTable.MEMBER, document.getUploadedBy().getId())),
        new AdminFieldResponse("created_at", AdminCell.time(document.getCreatedAt())),
        new AdminFieldResponse("updated_at", AdminCell.time(document.getUpdatedAt()))), List.of());
  }
}
