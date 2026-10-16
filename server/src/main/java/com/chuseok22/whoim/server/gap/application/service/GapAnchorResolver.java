package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.data.ResolvedAnchor;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class GapAnchorResolver {

  private static final Pattern LABEL_PATTERN = Pattern.compile("D([1-9][0-9]*)");

  // LLM이 준 값이라 어느 하나라도 맞지 않으면 예외 대신 빈 값을 돌려주고 호출자가 그 앵커만 버린다
  public Optional<ResolvedAnchor> resolve(List<EvidenceDocument> documents, String documentLabel, int startLine, int endLine) {
    Optional<EvidenceDocument> document = findDocument(documents, documentLabel);
    if (document.isEmpty()) {
      return Optional.empty();
    }
    String content = document.get().getContent();
    int[] lineStarts = lineStartOffsets(content);
    if (startLine < 1 || startLine > endLine || endLine > lineStarts.length) {
      return Optional.empty();
    }
    int startOffset = lineStarts[startLine - 1];
    int endOffset = endLine == lineStarts.length ? content.length() : lineStarts[endLine] - 1;
    if (content.substring(startOffset, endOffset).isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new ResolvedAnchor(document.get(), startOffset, endOffset, document.get().excerpt(startOffset, endOffset)));
  }

  private Optional<EvidenceDocument> findDocument(List<EvidenceDocument> documents, String documentLabel) {
    if (documentLabel == null) {
      return Optional.empty();
    }
    Matcher matcher = LABEL_PATTERN.matcher(documentLabel);
    if (!matcher.matches() || matcher.group(1).length() > 9) {
      return Optional.empty();
    }
    int documentIndex = Integer.parseInt(matcher.group(1)) - 1;
    return documentIndex < documents.size() ? Optional.of(documents.get(documentIndex)) : Optional.empty();
  }

  // 각 줄의 시작 오프셋. 개행 뒤 빈 줄도 한 줄로 센다(입력 줄 번호 규칙과 같다)
  private int[] lineStartOffsets(String content) {
    int lineCount = 1;
    for (int index = 0; index < content.length(); index++) {
      if (content.charAt(index) == '\n') {
        lineCount++;
      }
    }
    int[] starts = new int[lineCount];
    int line = 1;
    for (int index = 0; index < content.length(); index++) {
      if (content.charAt(index) == '\n') {
        starts[line++] = index + 1;
      }
    }
    return starts;
  }
}
