package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.data.ConvertedGap;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisResult;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnchorCandidate;
import com.chuseok22.whoim.server.gap.application.dto.data.GapCandidate;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GapAnalysisResultConverterTest {

  private final GapAnalysisResultConverter converter = new GapAnalysisResultConverter(new GapAnchorResolver());
  private final List<EvidenceDocument> documents = List.of(
      EvidenceDocument.builder().fileName("a.md").documentType(DocumentType.OTHER)
          .content("l1\nl2\nl3\nl4\nl5\nl6\nl7\nl8").build());

  @Test
  void convert_dropsDisallowedType() {
    GapAnalysisResult result = result(
        gap("TRIGGER_GAP", anchor(1, 1)),
        gap("choice_gap", anchor(2, 2)),
        gap("whatever", anchor(3, 3)));

    assertThat(converter.convert(result, documents)).isEmpty();
  }

  @Test
  void convert_dropsGapWithoutValidAnchors() {
    GapAnalysisResult result = result(gap("CHOICE_GAP", anchor(9, 9), anchor(3, 2)));

    assertThat(converter.convert(result, documents)).isEmpty();
  }

  @Test
  void convert_evidenceConflictNeedsTwoAnchors() {
    GapAnalysisResult result = result(
        gap("EVIDENCE_CONFLICT", anchor(1, 1)),
        gap("EVIDENCE_CONFLICT", anchor(2, 2), anchor(3, 3)));

    List<ConvertedGap> converted = converter.convert(result, documents);

    assertThat(converted).hasSize(1);
    assertThat(converted.get(0).type()).isEqualTo(GapType.EVIDENCE_CONFLICT);
    assertThat(converted.get(0).anchors()).hasSize(2);
  }

  @Test
  void convert_dedupesSameAnchorSet() {
    GapAnalysisResult result = result(
        gap("CHOICE_GAP", anchor(1, 1), anchor(2, 2)),
        gap("ASSESSMENT_GAP", anchor(1, 1), anchor(2, 2)),
        gap("ASSESSMENT_GAP", anchor(1, 1)));

    List<ConvertedGap> converted = converter.convert(result, documents);

    assertThat(converted).extracting(ConvertedGap::type).containsExactly(GapType.CHOICE_GAP, GapType.ASSESSMENT_GAP);
    assertThat(converted.get(1).anchors()).hasSize(1);
  }

  @Test
  void convert_capsAtFiveGaps() {
    GapAnchorCandidate[] anchors = IntStream.rangeClosed(1, 8).mapToObj(line -> anchor(line, line)).toArray(GapAnchorCandidate[]::new);
    GapCandidate[] gaps = IntStream.range(0, 8).mapToObj(index -> gap("CHOICE_GAP", anchors[index])).toArray(GapCandidate[]::new);

    List<ConvertedGap> converted = converter.convert(result(gaps), documents);

    assertThat(converted).hasSize(GapAnalysisResultConverter.MAX_GAPS_PER_ANALYSIS).hasSize(5);
    assertThat(converted.get(0).anchors().get(0).excerpt()).isEqualTo("l1");
  }

  @Test
  void convert_keepsValidGapWhenOtherAnchorInvalid() {
    GapAnalysisResult result = result(gap("CHOICE_GAP", anchor(99, 99), anchor(2, 2)));

    List<ConvertedGap> converted = converter.convert(result, documents);

    assertThat(converted).hasSize(1);
    assertThat(converted.get(0).anchors()).hasSize(1);
    assertThat(converted.get(0).anchors().get(0).excerpt()).isEqualTo("l2");
  }

  private static GapAnalysisResult result(GapCandidate... gaps) {
    return new GapAnalysisResult(List.of(gaps));
  }

  private static GapCandidate gap(String type, GapAnchorCandidate... anchors) {
    return new GapCandidate(type, "요약", "질문", List.of(anchors));
  }

  private static GapAnchorCandidate anchor(int startLine, int endLine) {
    return new GapAnchorCandidate("D1", startLine, endLine);
  }
}
