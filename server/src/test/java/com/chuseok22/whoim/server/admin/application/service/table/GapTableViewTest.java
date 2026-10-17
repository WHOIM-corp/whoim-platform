package com.chuseok22.whoim.server.admin.application.service.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
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
class GapTableViewTest {

  @Mock
  private GapRepository gapRepository;

  @Mock
  private GapAnchorRepository gapAnchorRepository;

  @Mock
  private AdminPackageOptions adminPackageOptions;

  @InjectMocks
  private GapTableView gapTableView;

  @Test
  void list_truncatesSummaryAndQuestion() {
    Gap gap = gap("요".repeat(100), "질".repeat(100));
    given(gapRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(gap)));

    AdminListResponse response = gapTableView.list(0, null);

    assertThat(response.columns()).containsExactly("id", "handover_package_id", "type", "summary", "question", "created_at");
    assertThat(response.rows().get(0).cells().get(3).text()).hasSize(81).endsWith("…");
    assertThat(response.rows().get(0).cells().get(4).text()).hasSize(81).endsWith("…");
  }

  @Test
  void list_withPackageId_usesPackageQuery() {
    UUID packageId = UUID.randomUUID();
    given(gapRepository.findAllByHandoverPackageId(eq(packageId), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of()));

    gapTableView.list(0, packageId);

    verify(gapRepository).findAllByHandoverPackageId(eq(packageId), any(Pageable.class));
  }

  @Test
  void detail_showsAnchorSectionWithFullExcerptAndDocumentLink() {
    Gap gap = gap("요약", "질문");
    EvidenceDocument document = EvidenceDocument.builder().build();
    UUID documentId = UUID.randomUUID();
    ReflectionTestUtils.setField(document, "id", documentId);
    String excerpt = "발".repeat(150);
    GapAnchor anchor = GapAnchor.builder().gap(gap).document(document).startOffset(3).endOffset(153).excerpt(excerpt).build();
    ReflectionTestUtils.setField(anchor, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(anchor, "createdAt", Instant.parse("2026-10-18T00:00:00Z"));
    given(gapRepository.findById(gap.getId())).willReturn(Optional.of(gap));
    given(gapAnchorRepository.findAllByGapOrderByCreatedAtAsc(gap)).willReturn(List.of(anchor));

    AdminDetailResponse response = gapTableView.detail(gap.getId());

    assertThat(response.sections()).hasSize(1);
    AdminSectionResponse section = response.sections().get(0);
    assertThat(section.title()).isEqualTo("근거 앵커 (gap_anchor)");
    assertThat(section.kind()).isEqualTo("TABLE");
    assertThat(section.columns()).containsExactly("id", "document_id", "start_offset", "end_offset", "excerpt", "created_at");
    var cells = section.rows().get(0).cells();
    assertThat(cells.get(1).linkTableName()).isEqualTo("evidence_document");
    assertThat(cells.get(1).linkId()).isEqualTo(documentId);
    assertThat(cells.get(4).text()).hasSize(150);
  }

  private Gap gap(String summary, String question) {
    HandoverPackage handoverPackage = HandoverPackage.builder().name("패키지").build();
    ReflectionTestUtils.setField(handoverPackage, "id", UUID.randomUUID());
    Gap gap = Gap.builder().handoverPackage(handoverPackage).type(GapType.values()[0])
        .summary(summary).question(question).build();
    ReflectionTestUtils.setField(gap, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(gap, "createdAt", Instant.parse("2026-10-18T00:00:00Z"));
    ReflectionTestUtils.setField(gap, "updatedAt", Instant.parse("2026-10-18T00:00:00Z"));
    return gap;
  }
}
