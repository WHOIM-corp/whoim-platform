package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.data.ConvertedGap;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisResult;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnchorCandidate;
import com.chuseok22.whoim.server.gap.application.dto.data.GapCandidate;
import com.chuseok22.whoim.server.gap.application.dto.data.ResolvedAnchor;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GapAnalysisResultConverter {

  static final int MAX_GAPS_PER_ANALYSIS = 5;
  private static final int MIN_ANCHORS_FOR_EVIDENCE_CONFLICT = 2;
  private static final Map<String, GapType> ALLOWED_TYPES = Map.of(
      GapType.CHOICE_GAP.name(), GapType.CHOICE_GAP,
      GapType.EVIDENCE_CONFLICT.name(), GapType.EVIDENCE_CONFLICT,
      GapType.ASSESSMENT_GAP.name(), GapType.ASSESSMENT_GAP);

  private final GapAnchorResolver gapAnchorResolver;

  public List<ConvertedGap> convert(GapAnalysisResult result, List<EvidenceDocument> documents) {
    List<ConvertedGap> converted = new ArrayList<>();
    Set<Set<String>> seenAnchorSets = new HashSet<>();
    int dropped = 0;
    for (GapCandidate candidate : result.gaps()) {
      ConvertedGap gap = convertCandidate(candidate, documents);
      if (gap == null || !seenAnchorSets.add(anchorKeys(gap, documents))) {
        dropped++;
        continue;
      }
      if (converted.size() < MAX_GAPS_PER_ANALYSIS) {
        converted.add(gap);
      } else {
        dropped++;
      }
    }
    if (dropped > 0) {
      log.warn("Gap 후보 {}건 제거(허용 유형·앵커·중복·개수 상한 기준)", dropped);
    }
    return converted;
  }

  private ConvertedGap convertCandidate(GapCandidate candidate, List<EvidenceDocument> documents) {
    GapType type = ALLOWED_TYPES.get(candidate.type());
    if (type == null) {
      return null;
    }
    List<ResolvedAnchor> anchors = new ArrayList<>();
    Set<String> seenAnchors = new HashSet<>();
    for (GapAnchorCandidate anchorCandidate : candidate.anchors()) {
      gapAnchorResolver.resolve(documents, anchorCandidate.document(), anchorCandidate.startLine(), anchorCandidate.endLine())
          .filter(anchor -> seenAnchors.add(anchorKey(anchor, documents)))
          .ifPresent(anchors::add);
    }
    int minimumAnchors = type == GapType.EVIDENCE_CONFLICT ? MIN_ANCHORS_FOR_EVIDENCE_CONFLICT : 1;
    if (anchors.size() < minimumAnchors) {
      return null;
    }
    return new ConvertedGap(type, candidate.summary(), candidate.question(), List.copyOf(anchors));
  }

  private Set<String> anchorKeys(ConvertedGap gap, List<EvidenceDocument> documents) {
    Set<String> keys = new HashSet<>();
    gap.anchors().forEach(anchor -> keys.add(anchorKey(anchor, documents)));
    return keys;
  }

  // 문서는 목록 위치(라벨)로 구분한다
  private String anchorKey(ResolvedAnchor anchor, List<EvidenceDocument> documents) {
    return documents.indexOf(anchor.document()) + ":" + anchor.startOffset() + ":" + anchor.endOffset();
  }
}
