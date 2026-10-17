package com.chuseok22.whoim.server.dlp.application.service;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.dlp.application.service.DlpAiDetector.DetectedSpan;
import com.chuseok22.whoim.server.llm.application.dto.data.GuardedInput;
import com.chuseok22.whoim.server.llm.application.service.LlmInputGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlpService implements LlmInputGuard {

  private final DlpRuleMasker ruleMasker;
  private final DlpAiDetector aiDetector;

  @Override
  public GuardedInput guard(String text) {
    if (text == null || text.isBlank()) {
      return new GuardedInput(text, UnaryOperator.identity());
    }
    DlpMaskingSession session = new DlpMaskingSession();
    String ruleMasked = maskByRules(text, session);
    String masked = applyAiSpans(ruleMasked, aiDetector.detect(ruleMasked), session);
    log.info("DLP 마스킹 완료: counts={}", session.counts());
    return new GuardedInput(masked, session::restore);
  }

  // 규칙 단계의 예기치 못한 예외도 Fail-Closed로 바꾼다. 원문이 담길 수 있어 cause와 메시지를 남기지 않는다
  private String maskByRules(String text, DlpMaskingSession session) {
    try {
      return ruleMasker.maskByRules(text, session);
    } catch (CustomException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.error("DLP 규칙 단계 실패: stage=rule, exception={}", exception.getClass().getSimpleName());
      throw new CustomException(ErrorCode.DLP_UNAVAILABLE);
    }
  }

  // 겹치는 구간은 앞선 구간을 우선하고, 위치가 밀리지 않도록 뒤에서부터 치환한다
  private String applyAiSpans(String text, List<DetectedSpan> spans, DlpMaskingSession session) {
    List<DetectedSpan> chosen = new ArrayList<>();
    int lastEnd = 0;
    for (DetectedSpan span : spans.stream()
        .sorted(Comparator.comparingInt(DetectedSpan::start).thenComparing(Comparator.comparingInt(DetectedSpan::end).reversed()))
        .toList()) {
      if (span.start() >= lastEnd) {
        chosen.add(span);
        lastEnd = span.end();
      }
    }
    StringBuilder masked = new StringBuilder(text);
    for (int index = chosen.size() - 1; index >= 0; index--) {
      DetectedSpan span = chosen.get(index);
      String token = session.tokenFor(text.substring(span.start(), span.end()), span.type());
      masked.replace(span.start(), span.end(), token);
    }
    return masked.toString();
  }
}
