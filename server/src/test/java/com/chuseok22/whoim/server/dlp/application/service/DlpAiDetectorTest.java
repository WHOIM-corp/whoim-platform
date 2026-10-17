package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.dlp.application.dto.data.DlpAiDetectionResult;
import com.chuseok22.whoim.server.dlp.application.dto.data.DlpDetection;
import com.chuseok22.whoim.server.dlp.application.service.DlpAiDetector.DetectedSpan;
import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.service.LlmChain;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import com.chuseok22.whoim.server.setting.core.constant.SettingKey;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DlpAiDetectorTest {

  private static final String INPUT = "담당자 홍길동이 다이아상사와 협의, 담당 [PERSON_01] 확인";

  @Mock
  private LlmChain chain;

  @Mock
  private SettingConfigService settingConfigService;

  private DlpAiDetector detector;

  @BeforeEach
  void setUp() {
    detector = new DlpAiDetector(chain, settingConfigService);
  }

  private void chainReturns(DlpDetection... detections) {
    when(settingConfigService.getString(SettingKey.PROMPT_DLP_DETECTION, DefaultPromptInstructions.DLP_DETECTION))
        .thenReturn(DefaultPromptInstructions.DLP_DETECTION);
    when(chain.generate(any(LlmRequest.class), eq(DlpAiDetectionResult.class)))
        .thenReturn(new DlpAiDetectionResult(List.of(detections)));
  }

  @Test
  void detect_returnsSpansOfDetectionsPresentInInput() {
    chainReturns(new DlpDetection("홍길동", "PERSON"), new DlpDetection("다이아상사", "CUSTOMER"));

    List<DetectedSpan> spans = detector.detect(INPUT);

    int person = INPUT.indexOf("홍길동");
    int customer = INPUT.indexOf("다이아상사");
    assertThat(spans).containsExactlyInAnyOrder(
        new DetectedSpan(person, person + 3, DlpEntityType.PERSON),
        new DetectedSpan(customer, customer + 5, DlpEntityType.CUSTOMER));
  }

  @Test
  void detect_dropsTextNotInInput() {
    chainReturns(new DlpDetection("없는문자열", "PERSON"));

    assertThat(detector.detect(INPUT)).isEmpty();
  }

  @Test
  void detect_dropsBlankAndSingleCharacter() {
    chainReturns(new DlpDetection("  ", "PERSON"), new DlpDetection("담", "PERSON"));

    assertThat(detector.detect(INPUT)).isEmpty();
  }

  @Test
  void detect_dropsWholeTokenString() {
    chainReturns(new DlpDetection("[PERSON_01]", "PERSON"));

    assertThat(detector.detect(INPUT)).isEmpty();
  }

  @Test
  void detect_dropsTextOverlappingToken() {
    chainReturns(new DlpDetection("01", "PERSON"), new DlpDetection("REDACTED", "SECRET"),
        new DlpDetection("PERSON_01", "PERSON"));

    assertThat(detector.detect(INPUT + " [REDACTED_EMAIL_01]")).isEmpty();
  }

  @Test
  void detect_returnsEveryOccurrence() {
    String input = "홍길동 만났고 나중에 홍길동 또 만남";
    chainReturns(new DlpDetection("홍길동", "PERSON"), new DlpDetection("홍길동", "PERSON"));

    List<DetectedSpan> spans = detector.detect(input);

    assertThat(spans).containsExactlyInAnyOrder(
        new DetectedSpan(0, 3, DlpEntityType.PERSON),
        new DetectedSpan(input.lastIndexOf("홍길동"), input.lastIndexOf("홍길동") + 3, DlpEntityType.PERSON));
  }

  @Test
  void detect_mapsUnknownTypeToOther() {
    chainReturns(new DlpDetection("홍길동", "WHATEVER"), new DlpDetection("다이아상사", null));

    assertThat(detector.detect(INPUT)).extracting(DetectedSpan::type)
        .containsOnly(DlpEntityType.OTHER).hasSize(2);
  }

  @Test
  void detect_throwsDlpUnavailable_whenChainFails() {
    when(settingConfigService.getString(SettingKey.PROMPT_DLP_DETECTION, DefaultPromptInstructions.DLP_DETECTION))
        .thenReturn(DefaultPromptInstructions.DLP_DETECTION);
    when(chain.generate(any(LlmRequest.class), eq(DlpAiDetectionResult.class)))
        .thenThrow(new CustomException(ErrorCode.LLM_UNAVAILABLE, new RuntimeException("boom")));

    assertThatThrownBy(() -> detector.detect(INPUT))
        .isInstanceOfSatisfying(CustomException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DLP_UNAVAILABLE);
          assertThat(exception.getCause()).isNull();
        });
  }

  @Test
  void detect_returnsEmpty_andSkipsChain_whenInputBlank() {
    assertThat(detector.detect("  ")).isEmpty();
    assertThat(detector.detect(null)).isEmpty();

    verifyNoInteractions(chain);
    verify(chain, never()).generate(any(LlmRequest.class), eq(DlpAiDetectionResult.class));
  }

  @Test
  void detect_usesInstructionFromSetting() {
    when(settingConfigService.getString(SettingKey.PROMPT_DLP_DETECTION, DefaultPromptInstructions.DLP_DETECTION))
        .thenReturn("커스텀 지침");
    when(chain.generate(any(LlmRequest.class), eq(DlpAiDetectionResult.class)))
        .thenReturn(new DlpAiDetectionResult(List.of()));

    detector.detect(INPUT);

    ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
    verify(chain).generate(captor.capture(), eq(DlpAiDetectionResult.class));
    assertThat(captor.getValue().system()).startsWith("커스텀 지침");
    assertThat(captor.getValue().prompt()).isEqualTo(INPUT);
  }

  @Test
  void detect_keepsTextInsideNonIssuedBrackets() {
    String input = "[ACME_CORP] 와 협의";
    chainReturns(new DlpDetection("ACME_CORP", "CUSTOMER"));

    int start = input.indexOf("ACME_CORP");
    assertThat(detector.detect(input))
        .containsExactly(new DetectedSpan(start, start + 9, DlpEntityType.CUSTOMER));
  }

  @Test
  void detect_dropsTextInsideIssuedTokenAndEvidenceLabel() {
    chainReturns(new DlpDetection("PERSON", "PERSON"), new DlpDetection("C1", "PERSON"));

    assertThat(detector.detect("[PERSON_01] 와 [C1] 근거")).isEmpty();
  }
}
