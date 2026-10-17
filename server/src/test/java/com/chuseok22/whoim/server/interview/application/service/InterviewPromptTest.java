package com.chuseok22.whoim.server.interview.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.gap.core.constant.GapType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.setting.core.constant.DefaultPromptInstructions;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InterviewPromptTest {

  private final Gap gap = Gap.builder().type(GapType.CHOICE_GAP).summary("재시도 5회 근거 없음").question("왜 5회로 했나요?").build();
  private final List<GapAnchor> anchors = List.of(GapAnchor.builder().gap(gap).excerpt("Retry 5회 설정").build());

  @Test
  void firstQuestion_containsGapInputsAndRules() {
    LlmRequest request = InterviewPrompt.firstQuestion(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION, gap, anchors);

    assertThat(request.prompt()).contains("재시도 5회 근거 없음", "왜 5회로 했나요?", "Retry 5회 설정");
    assertThat(request.system()).contains("JSON만 출력", "질문은 한 번에 하나", "유도", "추측", "\"question\"");
  }

  @Test
  void turn_containsOnlyAllowedInputsAndRules() {
    TraceState state = new TraceState(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.FILLED, "트래픽 급증", EvidenceLinkType.DIRECT, 1),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 1)));

    LlmRequest request = InterviewPrompt.turn(DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, state, "직전 질문입니다", "직전 답변입니다", 3);

    assertThat(request.prompt()).contains(
        "재시도 5회 근거 없음", "Retry 5회 설정", "CAUSE", "트래픽 급증", "RISK", "UNKNOWN",
        "직전 질문입니다", "직전 답변입니다", "[남은 턴 수]\n3", "SITUATION");
    assertThat(request.system()).contains(
        "JSON만 출력", "질문은 한 번에 하나", "유도", "모르겠다", "UNKNOWN", "추측", "source", "DIRECT", "RECORDED",
        "글자 그대로", "\"extracted\"", "\"nextQuestion\"");
  }

  @Test
  void firstQuestion_appendsFormatBlock_toDefaultInstruction() {
    LlmRequest request = InterviewPrompt.firstQuestion(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION, gap, anchors);

    assertThat(request.system()).startsWith(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION);
    assertThat(request.system()).contains("응답 형태:", "{\"question\":\"...\"}");
  }

  @Test
  void firstQuestion_appendsFormatBlock_toCustomInstruction() {
    LlmRequest request = InterviewPrompt.firstQuestion("커스텀", gap, anchors);

    assertThat(request.system()).startsWith("커스텀\n\n").contains("JSON만 출력", "응답 형태:", "\"question\"");
  }

  @Test
  void firstQuestion_appendsFormatBlock_whenInstructionIsEmptyString() {
    LlmRequest request = InterviewPrompt.firstQuestion("", gap, anchors);

    assertThat(request.system()).contains("JSON만 출력", "응답 형태:", "\"question\"");
  }

  @Test
  void firstQuestion_userPromptIsIndependentOfInstruction() {
    assertThat(InterviewPrompt.firstQuestion("커스텀", gap, anchors).prompt())
        .isEqualTo(InterviewPrompt.firstQuestion(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION, gap, anchors).prompt());
  }

  @Test
  void turn_appendsFormatBlock_toDefaultInstruction() {
    LlmRequest request = InterviewPrompt.turn(
        DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, TraceState.empty(), "질문", "답변", 3);

    assertThat(request.system()).startsWith(DefaultPromptInstructions.INTERVIEW_TURN);
    assertThat(request.system()).contains("응답 형태:", "{\"extracted\":[{\"element\":\"CAUSE\"");
  }

  @Test
  void turn_appendsFormatBlock_toCustomInstruction() {
    LlmRequest request = InterviewPrompt.turn("커스텀", gap, anchors, TraceState.empty(), "질문", "답변", 3);

    assertThat(request.system()).startsWith("커스텀\n\n").contains("JSON만 출력", "응답 형태:", "\"nextQuestion\"");
  }

  @Test
  void turn_appendsFormatBlock_whenInstructionIsEmptyString() {
    LlmRequest request = InterviewPrompt.turn("", gap, anchors, TraceState.empty(), "질문", "답변", 3);

    assertThat(request.system()).contains("JSON만 출력", "응답 형태:", "\"extracted\"");
  }

  @Test
  void turn_userPromptIsIndependentOfInstruction() {
    assertThat(InterviewPrompt.turn("커스텀", gap, anchors, TraceState.empty(), "질문", "답변", 3).prompt())
        .isEqualTo(InterviewPrompt.turn(
            DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, TraceState.empty(), "질문", "답변", 3).prompt());
  }
}
