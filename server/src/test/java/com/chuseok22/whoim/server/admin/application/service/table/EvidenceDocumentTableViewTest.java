package com.chuseok22.whoim.server.admin.application.service.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.evidence.infrastructure.repository.EvidenceDocumentRepository;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.member.core.constant.Role;
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
class EvidenceDocumentTableViewTest {

  @Mock
  private EvidenceDocumentRepository evidenceDocumentRepository;

  @Mock
  private AdminPackageOptions adminPackageOptions;

  @InjectMocks
  private EvidenceDocumentTableView evidenceDocumentTableView;

  @Test
  void list_truncatesContentButDetailKeepsFullText() {
    String content = "가".repeat(200);
    EvidenceDocument document = document(content);
    given(evidenceDocumentRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(document)));
    given(evidenceDocumentRepository.findById(document.getId())).willReturn(Optional.of(document));

    AdminListResponse list = evidenceDocumentTableView.list(0, null);
    AdminDetailResponse detail = evidenceDocumentTableView.detail(document.getId());

    assertThat(list.columns()).containsExactly(
        "id", "handover_package_id", "file_name", "document_type", "content", "uploaded_by_id", "created_at");
    String listed = list.rows().get(0).cells().get(4).text();
    assertThat(listed).hasSize(81).endsWith("…");
    String detailContent = detail.fields().stream()
        .filter(field -> "content".equals(field.label())).findFirst().orElseThrow().value().text();
    assertThat(detailContent).hasSize(200);
  }

  @Test
  void list_withPackageId_usesPackageQuery() {
    UUID packageId = UUID.randomUUID();
    given(evidenceDocumentRepository.findAllByHandoverPackageId(eq(packageId), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    AdminListResponse response = evidenceDocumentTableView.list(0, packageId);

    assertThat(response.selectedPackageId()).isEqualTo(packageId);
    verify(evidenceDocumentRepository).findAllByHandoverPackageId(eq(packageId), any(Pageable.class));
    verifyNoMoreInteractions(evidenceDocumentRepository);
  }

  @Test
  void list_withoutPackageId_usesFindAll() {
    given(evidenceDocumentRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

    evidenceDocumentTableView.list(0, null);

    verify(evidenceDocumentRepository).findAll(any(Pageable.class));
    verifyNoMoreInteractions(evidenceDocumentRepository);
  }

  @Test
  void detail_unknownId_throwsNotFound() {
    UUID unknownId = UUID.randomUUID();
    given(evidenceDocumentRepository.findById(unknownId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> evidenceDocumentTableView.detail(unknownId))
        .isInstanceOfSatisfying(CustomException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private EvidenceDocument document(String content) {
    Member uploader = Member.builder().username("u").password("p").name("업로더")
        .role(Role.USER).position(Position.PREDECESSOR).build();
    ReflectionTestUtils.setField(uploader, "id", UUID.randomUUID());
    HandoverPackage handoverPackage = HandoverPackage.builder().name("패키지").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    EvidenceDocument document = EvidenceDocument.builder()
        .handoverPackage(handoverPackage).fileName("a.txt").documentType(DocumentType.values()[0])
        .content(content).uploadedBy(uploader).build();
    ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(document, "createdAt", Instant.parse("2026-10-18T00:00:00Z"));
    ReflectionTestUtils.setField(document, "updatedAt", Instant.parse("2026-10-18T00:00:00Z"));
    return document;
  }
}
