package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.evidence.application.service.EvidenceDocumentService;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisInput;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GapAnalysisInputBuilderTest {

  private final EvidenceDocumentService evidenceDocumentService = Mockito.mock(EvidenceDocumentService.class);
  private final GapAnalysisInputBuilder builder = new GapAnalysisInputBuilder(evidenceDocumentService);

  @Test
  void build_prefixesNonBlankLinesWithPhysicalLineNumbers() {
    EvidenceDocument document = document("runbook.md", DocumentType.RUNBOOK, "a\n\nb");
    Mockito.when(evidenceDocumentService.toAiInputText(document)).thenReturn(document.getContent());

    GapAnalysisInput input = builder.build(List.of(document));

    assertThat(input.text()).contains("[D1:L1] a", "[D1:L3] b").doesNotContain("L2");
  }

  @Test
  void build_labelsDocumentsInGivenOrder() {
    EvidenceDocument first = document("first.md", DocumentType.RUNBOOK, "x");
    EvidenceDocument second = document("second.txt", DocumentType.INCIDENT, "y");
    Mockito.when(evidenceDocumentService.toAiInputText(first)).thenReturn("x");
    Mockito.when(evidenceDocumentService.toAiInputText(second)).thenReturn("y");

    GapAnalysisInput input = builder.build(List.of(first, second));

    assertThat(input.text()).contains("## D1 first.md (RUNBOOK)", "[D1:L1] x", "## D2 second.txt (INCIDENT)", "[D2:L1] y");
    assertThat(input.documents()).containsExactly(first, second);
  }

  private EvidenceDocument document(String fileName, DocumentType documentType, String content) {
    return EvidenceDocument.builder().fileName(fileName).documentType(documentType).content(content).build();
  }
}
