package com.chuseok22.whoim.server.dlp.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.dlp.application.service.DlpAiDetector.DetectedSpan;
import com.chuseok22.whoim.server.dlp.core.constant.DlpEntityType;
import com.chuseok22.whoim.server.dlp.infrastructure.properties.DlpProperties;
import com.chuseok22.whoim.server.llm.application.dto.data.GuardedInput;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

class DlpServiceTest {

  private final DlpAiDetector detector = mock(DlpAiDetector.class);
  private final DlpService service = new DlpService(
      new DlpRuleMasker(new DlpProperties(Map.of())), detector);
  private final Logger logger = (Logger) LoggerFactory.getLogger(DlpService.class);
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  @Test
  void guard_appliesRuleThenAi() {
    String ruleMasked = "서버 [REDACTED_IP_01] 담당은 홍길동";
    int start = ruleMasked.indexOf("홍길동");
    when(detector.detect(ruleMasked)).thenReturn(List.of(new DetectedSpan(start, start + 3, DlpEntityType.PERSON)));

    GuardedInput guarded = service.guard("서버 10.0.0.5 담당은 홍길동");

    assertThat(guarded.maskedText()).isEqualTo("서버 [REDACTED_IP_01] 담당은 [PERSON_01]");
  }

  @Test
  void guard_sendsRuleMaskedTextToDetector() {
    when(detector.detect(anyString())).thenReturn(List.of());

    service.guard("서버 10.0.0.5 점검");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(detector).detect(captor.capture());
    assertThat(captor.getValue()).doesNotContain("10.0.0.5").contains("[REDACTED_IP_01]");
  }

  @Test
  void guard_restorerRestoresPseudonymTokens() {
    String text = "담당은 홍길동";
    String ruleMasked = text;
    when(detector.detect(anyString())).thenReturn(
        List.of(new DetectedSpan(ruleMasked.indexOf("홍길동"), ruleMasked.length(), DlpEntityType.PERSON)));

    GuardedInput guarded = service.guard(text);

    assertThat(guarded.maskedText()).isEqualTo("담당은 [PERSON_01]");
    assertThat(guarded.restorer().apply("{\"who\":\"[PERSON_01]\"}")).isEqualTo("{\"who\":\"홍길동\"}");
  }

  @Test
  void guard_prefersEarlierSpan_whenSpansOverlap() {
    String text = "담당 홍길동수 확인";
    int start = text.indexOf("홍길동수");
    when(detector.detect(anyString())).thenReturn(List.of(
        new DetectedSpan(start, start + 4, DlpEntityType.PERSON),
        new DetectedSpan(start + 2, start + 4, DlpEntityType.PROJECT)));

    assertThat(service.guard(text).maskedText()).isEqualTo("담당 [PERSON_01] 확인");
  }

  @Test
  void guard_throwsDlpUnavailable_whenDetectorFails() {
    when(detector.detect(anyString())).thenThrow(new CustomException(ErrorCode.DLP_UNAVAILABLE));

    assertThatThrownBy(() -> service.guard("본문입니다"))
        .isInstanceOf(CustomException.class)
        .extracting(thrown -> ((CustomException) thrown).getErrorCode())
        .isEqualTo(ErrorCode.DLP_UNAVAILABLE);
  }

  @Test
  void guard_throwsDlpUnavailableWithoutCause_whenRuleStageFails() {
    DlpRuleMasker failingMasker = mock(DlpRuleMasker.class);
    when(failingMasker.maskByRules(anyString(), org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("비밀 원문"));
    DlpService failingService = new DlpService(failingMasker, detector);

    assertThatThrownBy(() -> failingService.guard("본문입니다"))
        .isInstanceOfSatisfying(CustomException.class, exception -> {
          assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DLP_UNAVAILABLE);
          assertThat(exception.getCause()).isNull();
          assertThat(exception.getMessage()).doesNotContain("비밀 원문");
        });
    verifyNoInteractions(detector);
  }

  @Test
  void guard_returnsBlankUnchanged_withoutCallingDetector() {
    GuardedInput guarded = service.guard("  ");

    assertThat(guarded.maskedText()).isEqualTo("  ");
    verifyNoInteractions(detector);
  }

  @Test
  void guard_logsOnlyTypesAndCounts() {
    String text = "서버 10.0.0.5 담당은 홍길동";
    when(detector.detect(anyString())).thenReturn(List.of());

    service.guard(text);

    assertThat(appender.list).isNotEmpty();
    assertThat(appender.list).allSatisfy(event -> {
      assertThat(event.getFormattedMessage()).doesNotContain("10.0.0.5").doesNotContain("홍길동");
    });
    assertThat(appender.list.get(0).getFormattedMessage()).contains("DLP 마스킹 완료").contains("IP_ADDRESS");
  }
}
