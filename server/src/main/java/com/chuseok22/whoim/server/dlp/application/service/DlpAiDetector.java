package com.chuseok22.whoim.server.dlp.application.service;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.dlp.application.dto.data.DlpAiDetectionResult;
import com.chuseok22.whoim.server.dlp.application.dto.data.DlpDetection;
import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmChain;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlpAiDetector {

  private static final int MINIMUM_TEXT_LENGTH = 2;
  // 발급 토큰과 상담 프롬프트의 근거 라벨([C1] 등)만 건드리지 않는다
  private static final Pattern EVIDENCE_LABEL = Pattern.compile("\\[C\\d+]");

  private final LlmChain chain;
  private final SettingConfigService settingConfigService;

  // Lombok 설정에 @Qualifier 복사가 없어 같은 타입 빈과 구분하도록 생성자를 직접 쓴다
  public DlpAiDetector(
      @Qualifier("detectionChain") LlmChain chain,
      SettingConfigService settingConfigService
  ) {
    this.chain = chain;
    this.settingConfigService = settingConfigService;
  }

  // AI 응답이 아니라 입력 안의 실제 출현 구간만 돌려줘 기존 토큰이나 지어낸 문자열이 영향을 주지 못하게 한다
  public List<DetectedSpan> detect(String maskedText) {
    if (maskedText == null || maskedText.isBlank()) {
      return List.of();
    }
    String instruction = settingConfigService.getString(
        SettingKey.PROMPT_DLP_DETECTION, DefaultPromptInstructions.DLP_DETECTION);
    DlpAiDetectionResult result = requestDetections(DlpDetectionPrompt.build(instruction, maskedText));

    List<int[]> tokenRanges = findTokenRanges(maskedText);
    List<DetectedSpan> spans = new ArrayList<>();
    Set<String> handledTexts = new HashSet<>();
    for (DlpDetection detection : result.detections()) {
      String text = detection.text();
      if (text == null || text.length() < MINIMUM_TEXT_LENGTH || !handledTexts.add(text)) {
        continue;
      }
      DlpEntityType type = DlpEntityType.fromAiLabel(detection.type());
      collectOccurrences(maskedText, text, type, tokenRanges, spans);
    }
    log.info("DLP AI 탐지 완료: detections={}, spans={}", result.detections().size(), spans.size());
    return spans;
  }

  private DlpAiDetectionResult requestDetections(LlmRequest request) {
    try {
      return chain.generate(request, DlpAiDetectionResult.class);
    } catch (CustomException exception) {
      if (exception.getErrorCode() != ErrorCode.LLM_UNAVAILABLE) {
        throw exception;
      }
      // 원인에 입력 일부가 담길 수 있어 cause 없이 바꾼다
      throw new CustomException(ErrorCode.DLP_UNAVAILABLE);
    }
  }

  private void collectOccurrences(
      String maskedText,
      String text,
      DlpEntityType type,
      List<int[]> tokenRanges,
      List<DetectedSpan> spans
  ) {
    int from = maskedText.indexOf(text);
    while (from >= 0) {
      int end = from + text.length();
      if (!overlapsToken(from, end, tokenRanges)) {
        spans.add(new DetectedSpan(from, end, type));
      }
      from = maskedText.indexOf(text, from + 1);
    }
  }

  private List<int[]> findTokenRanges(String maskedText) {
    List<int[]> ranges = new ArrayList<>();
    for (Pattern pattern : List.of(DlpMaskingSession.ISSUED_TOKEN, EVIDENCE_LABEL)) {
      Matcher matcher = pattern.matcher(maskedText);
      while (matcher.find()) {
        ranges.add(new int[]{matcher.start(), matcher.end()});
      }
    }
    return ranges;
  }

  private boolean overlapsToken(int start, int end, List<int[]> tokenRanges) {
    return tokenRanges.stream().anyMatch(range -> start < range[1] && range[0] < end);
  }

  public record DetectedSpan(int start, int end, DlpEntityType type) {
  }
}
