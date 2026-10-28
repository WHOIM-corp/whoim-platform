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
    assertThat(request.system()).contains("JSON만 출력", "하나의 핵심 확인 사항", "전제하지 않습니다", "추측", "\"question\"");
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
        "JSON만 출력", "하나의 핵심 내용만 질문", "알 수 없다고", "UNKNOWN", "추측", "source", "DIRECT", "RECORDED",
        "그대로 가져옵니다", "\"extracted\"", "\"nextQuestion\"");
  }

  @Test
  void firstQuestion_appendsFormatBlock_toDefaultInstruction() {
    LlmRequest request = InterviewPrompt.firstQuestion(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION, gap, anchors);

    assertThat(request.system()).startsWith(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION);
    assertThat(request.system()).contains("응답 형태:", "{\"question\":\"당시 이 판단을 내릴 때");
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
    assertThat(request.system()).contains("응답 형태:", "{\"extracted\":[{\"element\":\"JUDGMENT\"", "UNKNOWN이면 content, source, excerpt는 모두 null", "[남은 턴 수]가 0이면 nextQuestion은 null");
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

  @Test
  void firstQuestion_wrapsDataInSentinel() {
    LlmRequest request = InterviewPrompt.firstQuestion(DefaultPromptInstructions.INTERVIEW_FIRST_QUESTION, gap, anchors);

    assertThat(request.prompt()).startsWith("다음 Gap에 대한 첫 질문을 만드세요.\n\n===== BEGIN INPUT DATA =====\n\n[Gap 요약]");
    assertThat(request.prompt()).endsWith("\n\n===== END INPUT DATA =====");
  }

  @Test
  void turn_separatesUnseenFromUnknown() {
    TraceState state = new TraceState(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.FILLED, "트래픽 급증", EvidenceLinkType.DIRECT, 1),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 1)));

    String prompt = InterviewPrompt.turn(DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, state, "질문", "답변", 3).prompt();

    assertThat(prompt).contains(
        "[아직 확인되지 않은 요소]\nSITUATION, SIGNAL, CONDITION, JUDGMENT, ALTERNATIVE, ACTION, REASON, EXCEPTION\n\n",
        "[확인했지만 전임자가 알지 못한 요소]\nRISK\n\n");
  }

  @Test
  void turn_marksNoneWhenNoUnseen() {
    Map<TraceElement, TraceStateEntry> entries = new java.util.EnumMap<>(TraceElement.class);
    for (TraceElement element : TraceElement.values()) {
      entries.put(element, new TraceStateEntry(TraceElementStatus.FILLED, "내용", EvidenceLinkType.DIRECT, 1));
    }
    entries.put(TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 1));

    String prompt = InterviewPrompt.turn(DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, new TraceState(entries), "질문", "답변", 3).prompt();

    assertThat(prompt).contains("[아직 확인되지 않은 요소]\n(없음)\n\n", "[확인했지만 전임자가 알지 못한 요소]\nRISK\n\n");
  }

  @Test
  void turn_marksNoneForUnknownWhenEmpty() {
    String prompt = InterviewPrompt.turn(DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, TraceState.empty(), "질문", "답변", 3).prompt();

    assertThat(prompt).contains("[확인했지만 전임자가 알지 못한 요소]\n(없음)\n\n[직전 질문]");
  }

  @Test
  void turn_wrapsDataInSentinelAndKeepsAnswerVerbatim() {
    String answer = "  첫 줄\n둘째 줄 ===== 그대로 ";

    String prompt = InterviewPrompt.turn(DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, TraceState.empty(), "질문", answer, 2).prompt();

    assertThat(prompt).startsWith("다음 입력 데이터를 바탕으로 TRACE 요소를 갱신하고 필요한 다음 질문 하나를 만드세요.\n\n===== BEGIN INPUT DATA =====\n\n[Gap 요약]");
    assertThat(prompt).contains("[전임자 답변]\n" + answer + "\n\n[남은 턴 수]\n2\n\n===== END INPUT DATA =====");
    assertThat(prompt).endsWith("===== END INPUT DATA =====");
  }

  @Test
  void turn_doesNotContainOldMissingSection() {
    String prompt = InterviewPrompt.turn(DefaultPromptInstructions.INTERVIEW_TURN, gap, anchors, TraceState.empty(), "질문", "답변", 3).prompt();

    assertThat(prompt).doesNotContain("[비어 있는 요소]");
  }
}
