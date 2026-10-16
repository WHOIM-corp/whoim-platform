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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InterviewPromptTest {

  private final Gap gap = Gap.builder().type(GapType.CHOICE_GAP).summary("재시도 5회 근거 없음").question("왜 5회로 했나요?").build();
  private final List<GapAnchor> anchors = List.of(GapAnchor.builder().gap(gap).excerpt("Retry 5회 설정").build());

  @Test
  void firstQuestion_containsGapInputsAndRules() {
    LlmRequest request = InterviewPrompt.firstQuestion(gap, anchors);

    assertThat(request.prompt()).contains("재시도 5회 근거 없음", "왜 5회로 했나요?", "Retry 5회 설정");
    assertThat(request.system()).contains("JSON만 출력", "질문은 한 번에 하나", "유도", "추측", "\"question\"");
  }

  @Test
  void turn_containsOnlyAllowedInputsAndRules() {
    TraceState state = new TraceState(Map.of(
        TraceElement.CAUSE, new TraceStateEntry(TraceElementStatus.FILLED, "트래픽 급증", EvidenceLinkType.DIRECT, 1),
        TraceElement.RISK, new TraceStateEntry(TraceElementStatus.UNKNOWN, null, null, 1)));

    LlmRequest request = InterviewPrompt.turn(gap, anchors, state, "직전 질문입니다", "직전 답변입니다", 3);

    assertThat(request.prompt()).contains(
        "재시도 5회 근거 없음", "Retry 5회 설정", "CAUSE", "트래픽 급증", "RISK", "UNKNOWN",
        "직전 질문입니다", "직전 답변입니다", "[남은 턴 수]\n3", "SITUATION");
    assertThat(request.system()).contains(
        "JSON만 출력", "질문은 한 번에 하나", "유도", "모르겠다", "UNKNOWN", "추측", "source", "DIRECT", "RECORDED",
        "글자 그대로", "\"extracted\"", "\"nextQuestion\"");
  }
}
