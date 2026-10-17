package com.chuseok22.whoim.server.dlp.application.service;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.dlp.infrastructure.properties.DlpProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// NFKC 정규화 없이 정규식이 전각 문자를 직접 받는다. 정규화는 원문 길이와 위치를 바꿔 복원을 어렵게 한다
@Component
public class DlpRuleMasker {

  private static final String DIGIT = "[0-9０-９]";
  private static final String HYPHEN = "[-－‐–−]";
  private static final String AT = "[@＠]";
  private static final String DOT = "[.．]";
  private static final String ALPHANUMERIC = "A-Za-z0-9Ａ-Ｚａ-ｚ０-９";

  private static final List<Rule> RULES = List.of(
      new Rule(DlpEntityType.EMAIL, Pattern.compile(
          "[" + ALPHANUMERIC + "._%+\\-]+" + AT + "[" + ALPHANUMERIC + "\\-]+(?:" + DOT + "[" + ALPHANUMERIC
              + "\\-]+)*" + DOT + "[" + ALPHANUMERIC + "]{2,}"), value -> true),
      new Rule(DlpEntityType.IP_ADDRESS, Pattern.compile(
          "(?<!" + DIGIT + "|" + DOT + ")(?:" + DIGIT + "{1,3}" + DOT + "){3}" + DIGIT + "{1,3}(?!" + DIGIT + "|"
              + DOT + DIGIT + ")"), DlpRuleMasker::isValidIpAddress),
      new Rule(DlpEntityType.PHONE_NUMBER, Pattern.compile(
          "(?<!" + DIGIT + ")(?:[0０][1１]" + DIGIT + "[-－‐–− ]?" + DIGIT + "{3,4}[-－‐–− ]?" + DIGIT + "{4}"
              + "|[0０]" + DIGIT + "{1,2}[-－‐–− ]" + DIGIT + "{3,4}[-－‐–− ]" + DIGIT + "{4})(?!" + DIGIT + ")"),
          value -> true),
      new Rule(DlpEntityType.RESIDENT_REGISTRATION_NUMBER, Pattern.compile(
          "(?<!" + DIGIT + ")" + DIGIT + "{6}" + HYPHEN + DIGIT + "{7}(?!" + DIGIT + ")"),
          DlpRuleMasker::isValidResidentRegistrationNumber),
      new Rule(DlpEntityType.API_KEY, Pattern.compile(
          "(?<![A-Za-z0-9_])(?:sk-|AKIA|ghp_|AIza|xox[baprs]-)[A-Za-z0-9_\\-]{16,}"), value -> true),
      new Rule(DlpEntityType.API_KEY, Pattern.compile("(?<![A-Za-z0-9])[A-Za-z0-9]{32,}(?![A-Za-z0-9])"),
          value -> value.chars().anyMatch(Character::isLetter) && value.chars().anyMatch(Character::isDigit)));

  private final Pattern dictionaryPattern;
  private final Map<String, DlpEntityType> dictionaryTypesByTerm;

  public DlpRuleMasker(DlpProperties properties) {
    Map<String, DlpEntityType> typesByTerm = new HashMap<>();
    properties.entities().forEach((type, terms) -> terms.stream()
        .filter(term -> !term.isBlank())
        .forEach(term -> typesByTerm.putIfAbsent(term.strip().toLowerCase(Locale.ROOT), type)));
    this.dictionaryTypesByTerm = Map.copyOf(typesByTerm);
    this.dictionaryPattern = typesByTerm.isEmpty() ? null : compileDictionary(properties);
  }

  public String maskByRules(String text, DlpMaskingSession session) {
    List<Span> spans = collectSpans(text);
    StringBuilder masked = new StringBuilder();
    int cursor = 0;
    for (Span span : spans) {
      masked.append(text, cursor, span.start()).append(session.tokenFor(text.substring(span.start(), span.end()), span.type()));
      cursor = span.end();
    }
    return masked.append(text, cursor, text.length()).toString();
  }

  private List<Span> collectSpans(String text) {
    List<Span> tokenSpans = new ArrayList<>();
    Matcher tokenMatcher = DlpMaskingSession.ISSUED_TOKEN.matcher(text);
    while (tokenMatcher.find()) {
      tokenSpans.add(new Span(tokenMatcher.start(), tokenMatcher.end(), DlpEntityType.OTHER));
    }

    List<Span> candidates = new ArrayList<>();
    for (Rule rule : RULES) {
      Matcher matcher = rule.pattern().matcher(text);
      while (matcher.find()) {
        if (rule.validator().test(matcher.group())) {
          candidates.add(new Span(matcher.start(), matcher.end(), rule.type()));
        }
      }
    }
    if (dictionaryPattern != null) {
      Matcher matcher = dictionaryPattern.matcher(text);
      while (matcher.find()) {
        candidates.add(new Span(matcher.start(), matcher.end(),
            dictionaryTypesByTerm.getOrDefault(matcher.group().strip().toLowerCase(Locale.ROOT), DlpEntityType.OTHER)));
      }
    }

    // 이미 토큰인 구간과 겹치는 후보는 버려 토큰이 다시 치환되지 않게 한다
    List<Span> remaining = candidates.stream()
        .filter(candidate -> tokenSpans.stream().noneMatch(candidate::overlaps))
        .sorted(Comparator.comparingInt(Span::start).thenComparing(Comparator.comparingInt(Span::length).reversed()))
        .toList();
    List<Span> accepted = new ArrayList<>();
    int lastEnd = 0;
    for (Span candidate : remaining) {
      if (candidate.start() >= lastEnd) {
        accepted.add(candidate);
        lastEnd = candidate.end();
      }
    }
    return accepted;
  }

  // 긴 용어를 앞에 두어 'A사업부'가 'A사'보다 먼저 일치하게 한다
  private static Pattern compileDictionary(DlpProperties properties) {
    String alternation = properties.entities().values().stream()
        .flatMap(List::stream)
        .filter(term -> !term.isBlank())
        .map(String::strip)
        .distinct()
        .sorted(Comparator.comparingInt(String::length).reversed())
        .map(Pattern::quote)
        .collect(Collectors.joining("|"));
    return Pattern.compile(alternation, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

  private static boolean isValidIpAddress(String value) {
    String[] octets = value.split("[.．]");
    for (String octet : octets) {
      if (toNumber(octet) > 255) {
        return false;
      }
    }
    return octets.length == 4;
  }

  private static boolean isValidResidentRegistrationNumber(String value) {
    int month = toNumber(value.substring(2, 4));
    int day = toNumber(value.substring(4, 6));
    return month >= 1 && month <= 12 && day >= 1 && day <= 31;
  }

  // 전각 숫자도 Character.digit이 값으로 바꿔 준다
  private static int toNumber(String digits) {
    int number = 0;
    for (char character : digits.toCharArray()) {
      number = number * 10 + Character.digit(character, 10);
    }
    return number;
  }

  private record Rule(DlpEntityType type, Pattern pattern, Predicate<String> validator) {

  }

  private record Span(int start, int end, DlpEntityType type) {

    int length() {
      return end - start;
    }

    boolean overlaps(Span other) {
      return start < other.end && other.start < end;
    }
  }
}
