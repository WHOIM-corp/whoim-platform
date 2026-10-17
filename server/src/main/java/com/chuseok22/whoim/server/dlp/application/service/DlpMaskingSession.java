package com.chuseok22.whoim.server.dlp.application.service;

import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tools.jackson.core.io.JsonStringEncoder;

// 한 번의 LLM 호출 안에서 원문과 토큰의 매핑을 메모리로만 들고 있다. 외부로 나가지 않고 로그에도 남기지 않는다
public class DlpMaskingSession {

  // 이 세션이 발급하는 토큰 모양만 일치한다. 문서 안의 [PHOENIX] 같은 대괄호 단어는 토큰으로 보지 않는다
  public static final Pattern ISSUED_TOKEN = Pattern.compile("\\[(?:REDACTED_)?(?:"
      + Arrays.stream(DlpEntityType.values()).map(DlpEntityType::label).collect(Collectors.joining("|"))
      + ")_\\d{2,}]");

  private final Map<TokenKey, String> tokensByValue = new HashMap<>();
  private final Map<String, String> originalsByToken = new LinkedHashMap<>();
  private final Map<DlpEntityType, Integer> sequenceByType = new EnumMap<>(DlpEntityType.class);

  public String tokenFor(String original, DlpEntityType type) {
    TokenKey key = new TokenKey(type, original.toLowerCase(Locale.ROOT));
    String existing = tokensByValue.get(key);
    if (existing != null) {
      return existing;
    }
    int sequence = sequenceByType.merge(type, 1, Integer::sum);
    String body = "%s_%02d".formatted(type.label(), sequence);
    String token = type.isPseudonymized() ? "[" + body + "]" : "[REDACTED_" + body + "]";
    tokensByValue.put(key, token);
    originalsByToken.put(token, original);
    return token;
  }

  // 응답이 JSON 문자열 안에 토큰을 담는다는 전제라서 원문을 JSON 이스케이프해서 끼워 넣는다
  public String restore(String text) {
    if (originalsByToken.isEmpty()) {
      return text;
    }
    String alternation = originalsByToken.keySet().stream()
        .sorted(Comparator.comparingInt(String::length).reversed())
        .map(Pattern::quote)
        .collect(Collectors.joining("|"));
    Matcher matcher = Pattern.compile(alternation).matcher(text);
    StringBuilder restored = new StringBuilder();
    while (matcher.find()) {
      String original = originalsByToken.get(matcher.group());
      StringBuilder escaped = new StringBuilder();
      JsonStringEncoder.getInstance().quoteAsString(original, escaped);
      matcher.appendReplacement(restored, Matcher.quoteReplacement(escaped.toString()));
    }
    matcher.appendTail(restored);
    return restored.toString();
  }

  public Map<DlpEntityType, Integer> counts() {
    return Map.copyOf(sequenceByType);
  }

  // 원문이 로그·예외 메시지로 새지 않도록 개수만 노출한다
  @Override
  public String toString() {
    return "DlpMaskingSession[counts=" + sequenceByType + "]";
  }

  private record TokenKey(DlpEntityType type, String lowerCasedValue) {

  }
}
