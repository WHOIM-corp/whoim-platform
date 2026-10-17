package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.evidence.application.service.EvidenceDocumentService;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisInput;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GapAnalysisInputBuilder {

  private final EvidenceDocumentService evidenceDocumentService;

  public GapAnalysisInput build(List<EvidenceDocument> documentsInUploadOrder) {
    StringBuilder text = new StringBuilder();
    for (int index = 0; index < documentsInUploadOrder.size(); index++) {
      EvidenceDocument document = documentsInUploadOrder.get(index);
      String label = "D" + (index + 1);
      text.append("## ").append(label).append(' ')
          .append(document.getFileName()).append(" (").append(document.getDocumentType()).append(")\n");
      // toAiInputText 결과(DLP 적용 후 포함)는 content와 같은 줄 구조여야 한다. 줄 번호가 앵커 변환의 기준이기 때문이다
      String[] lines = evidenceDocumentService.toAiInputText(document).split("\n", -1);
      for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
        if (!lines[lineIndex].isBlank()) {
          text.append('[').append(label).append(":L").append(lineIndex + 1).append("] ").append(lines[lineIndex]).append('\n');
        }
      }
    }
    return new GapAnalysisInput(text.toString(), List.copyOf(documentsInUploadOrder));
  }
}
