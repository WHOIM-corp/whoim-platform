package com.chuseok22.whoim.server.common.application.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExcerptMatcherTest {

  @Test
  void findIn_normalizesWhitespaceAndNewlines_returnsOriginalSpan() {
    String source = "앞 문장\n재고가   부족하면\n  발주한다. 뒤";

    assertThat(ExcerptMatcher.findIn(source, "재고가 부족하면 발주한다."))
        .contains("재고가   부족하면\n  발주한다.");
  }

  @Test
  void findIn_handlesRegexSpecialCharacters() {
    assertThat(ExcerptMatcher.findIn("비용은 (a+b)*2 [원] 이다", "(a+b)*2 [원]")).contains("(a+b)*2 [원]");
  }

  @Test
  void findIn_blankOrNullExcerptIsEmpty() {
    assertThat(ExcerptMatcher.findIn("내용", "  \n ")).isEmpty();
    assertThat(ExcerptMatcher.findIn("내용", null)).isEmpty();
  }

  @Test
  void findIn_partialMismatchIsEmpty() {
    assertThat(ExcerptMatcher.findIn("재고가 부족하면 발주한다", "재고가 충분하면 발주한다")).isEmpty();
  }
}
