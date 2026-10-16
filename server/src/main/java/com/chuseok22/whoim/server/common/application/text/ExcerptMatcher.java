package com.chuseok22.whoim.server.common.application.text;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ExcerptMatcher {

  private ExcerptMatcher() {
  }

  // 연속 공백·줄바꿈 차이를 허용해 찾고, 저장용으로 source의 원문 구간을 돌려준다
  public static Optional<String> findIn(String source, String excerpt) {
    if (source == null || excerpt == null || excerpt.isBlank()) {
      return Optional.empty();
    }
    String regex = Arrays.stream(excerpt.trim().split("\\s+"))
        .map(Pattern::quote)
        .collect(Collectors.joining("\\s+"));
    Matcher matcher = Pattern.compile(regex).matcher(source);
    return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
  }
}
