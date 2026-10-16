package com.chuseok22.whoim.server.evidence.infrastructure.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class EvidenceDocumentTest {

  private EvidenceDocument documentWith(String content) {
    return EvidenceDocument.builder().content(content).build();
  }

  @Test
  void excerpt_returnsHalfOpenRange() {
    EvidenceDocument document = documentWith("abcdef");

    assertThat(document.excerpt(0, 6)).isEqualTo("abcdef");
    assertThat(document.excerpt(2, 4)).isEqualTo("cd");
    assertThat(document.excerpt(5, 6)).isEqualTo("f");
  }

  @Test
  void excerpt_handlesKorean() {
    assertThat(documentWith("가나다라").excerpt(1, 3)).isEqualTo("나다");
  }

  @Test
  void excerpt_invalidRange_throwsRangeInvalid() {
    EvidenceDocument document = documentWith("abcdef");

    int[][] invalidRanges = {{3, 3}, {4, 2}, {-1, 2}, {0, 7}};
    for (int[] range : invalidRanges) {
      assertThatThrownBy(() -> document.excerpt(range[0], range[1]))
          .isInstanceOfSatisfying(CustomException.class,
              e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EVIDENCE_EXCERPT_RANGE_INVALID));
    }
  }
}
