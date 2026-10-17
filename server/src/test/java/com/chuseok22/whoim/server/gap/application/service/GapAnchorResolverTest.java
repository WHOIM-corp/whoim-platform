package com.chuseok22.whoim.server.gap.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.gap.application.dto.data.ResolvedAnchor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GapAnchorResolverTest {

  private final GapAnchorResolver resolver = new GapAnchorResolver();
  private final EvidenceDocument document = document("one\ntwo\n\nfour");
  private final List<EvidenceDocument> documents = List.of(document);

  @Test
  void resolve_singleLine() {
    ResolvedAnchor anchor = resolver.resolve(documents, "D1", 2, 2).orElseThrow();

    assertThat(anchor.document()).isSameAs(document);
    assertThat(anchor.startOffset()).isEqualTo(4);
    assertThat(anchor.endOffset()).isEqualTo(7);
    assertThat(anchor.excerpt()).isEqualTo("two");
    assertThat(document.getContent().substring(anchor.startOffset(), anchor.endOffset())).isEqualTo(anchor.excerpt());
  }

  @Test
  void resolve_multiLineRange() {
    ResolvedAnchor anchor = resolver.resolve(documents, "D1", 1, 2).orElseThrow();

    assertThat(anchor.excerpt()).isEqualTo("one\ntwo");
    assertThat(anchor.startOffset()).isZero();
    assertThat(anchor.endOffset()).isEqualTo(7);
  }

  @Test
  void resolve_lastLineWithoutTrailingNewline() {
    ResolvedAnchor anchor = resolver.resolve(documents, "D1", 4, 4).orElseThrow();

    assertThat(anchor.excerpt()).isEqualTo("four");
    assertThat(anchor.endOffset()).isEqualTo(document.getContent().length());
  }

  @Test
  void resolve_rejectsStartAfterEnd() {
    assertThat(resolver.resolve(documents, "D1", 3, 2)).isEmpty();
  }

  @Test
  void resolve_rejectsZeroOrNegativeLine() {
    assertThat(resolver.resolve(documents, "D1", 0, 1)).isEmpty();
    assertThat(resolver.resolve(documents, "D1", -1, 1)).isEmpty();
  }

  @Test
  void resolve_rejectsLineBeyondEnd() {
    assertThat(resolver.resolve(documents, "D1", 4, 5)).isEmpty();
    assertThat(resolver.resolve(documents, "D1", 5, 5)).isEmpty();
  }

  @Test
  void resolve_rejectsUnknownLabel() {
    assertThat(resolver.resolve(documents, "D9", 1, 1)).isEmpty();
    assertThat(resolver.resolve(documents, "X1", 1, 1)).isEmpty();
    assertThat(resolver.resolve(documents, "D0", 1, 1)).isEmpty();
    assertThat(resolver.resolve(documents, null, 1, 1)).isEmpty();
  }

  @Test
  void resolve_rejectsBlankRange() {
    Optional<ResolvedAnchor> anchor = resolver.resolve(documents, "D1", 3, 3);

    assertThat(anchor).isEmpty();
  }

  private static EvidenceDocument document(String content) {
    return EvidenceDocument.builder().fileName("a.md").documentType(DocumentType.OTHER).content(content).build();
  }
}
