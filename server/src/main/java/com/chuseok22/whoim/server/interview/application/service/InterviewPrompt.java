package com.chuseok22.whoim.server.interview.application.service;

import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import java.util.List;
import java.util.stream.Collectors;

public final class InterviewPrompt {

  private static final int MAX_CONTENT_SUMMARY_LENGTH = 80;

  private static final String COMMON_RULES = """
      당신은 전임자의 암묵적 판단을 끌어내는 인터뷰어입니다. 판단의 근거와 맥락을 묻되 답을 유도하지 않습니다.
      TRACE 요소는 SITUATION, SIGNAL, CAUSE, CONDITION, JUDGMENT, ALTERNATIVE, ACTION, REASON, EXCEPTION, RISK 10개입니다.

      규칙:
      - 질문은 한 번에 하나만 합니다. 정답을 암시하거나 유도하는 표현을 쓰지 않습니다.
      - 근거는 주어진 기록 발췌와 전임자의 답변 안에서만 고릅니다. 모르면 비워 두고 추측하지 않습니다.
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      """;

  private static final String FIRST_QUESTION_SYSTEM = COMMON_RULES + """
      - 주어진 Gap에 대해 전임자에게 처음 던질 열린 질문 하나를 만듭니다. 질문은 한국어로 씁니다.

      응답 형태:
      {"question":"..."}
      """;

  private static final String TURN_SYSTEM = COMMON_RULES + """
      - 전임자의 마지막 답변에서 TRACE 요소를 추출합니다. 답변에 없는 내용은 추출하지 않습니다.
      - 전임자가 "모르겠다"고 답한 요소는 status를 UNKNOWN으로 추출하고 content, source, excerpt는 비웁니다. 내용을 지어내지 않습니다.
      - status가 FILLED이면 source(DIRECT 또는 RECORDED)와 excerpt가 반드시 있어야 합니다.
        DIRECT는 전임자 답변, RECORDED는 기록 발췌에서 가져오며, excerpt는 해당 원문에서 글자 그대로 복사한 구절이어야 합니다.
      - nextQuestion은 아직 비어 있는 요소 중 가장 중요한 것을 묻는 한국어 질문 하나입니다.

      응답 형태:
      {"extracted":[{"element":"CAUSE","status":"FILLED","content":"...","source":"DIRECT","excerpt":"..."}],"nextQuestion":"..."}
      """;

  private InterviewPrompt() {
  }

  public static LlmRequest firstQuestion(Gap gap, List<GapAnchor> anchors) {
    String prompt = "다음 Gap에 대한 첫 질문을 만드세요.\n\n" + gapSection(gap, anchors);
    return new LlmRequest(FIRST_QUESTION_SYSTEM, prompt, null, null);
  }

  public static LlmRequest turn(
      Gap gap, List<GapAnchor> anchors, TraceState state, String previousQuestion, String answer, int remainingTurns) {
    String prompt = "다음 답변에서 TRACE 요소를 추출하고 다음 질문을 만드세요.\n\n"
        + gapSection(gap, anchors)
        + "\n[현재 TRACE 상태]\n" + stateSummary(state)
        + "\n[비어 있는 요소]\n" + missingSummary(state)
        + "\n[직전 질문]\n" + previousQuestion
        + "\n\n[전임자 답변]\n" + answer
        + "\n\n[남은 턴 수]\n" + remainingTurns + "\n";
    return new LlmRequest(TURN_SYSTEM, prompt, null, null);
  }

  private static String gapSection(Gap gap, List<GapAnchor> anchors) {
    String excerpts = anchors.stream()
        .map(anchor -> "- " + anchor.getExcerpt())
        .collect(Collectors.joining("\n"));
    return "[Gap 요약]\n" + gap.getSummary()
        + "\n\n[질문 후보]\n" + gap.getQuestion()
        + "\n\n[기록 발췌]\n" + excerpts + "\n";
  }

  private static String stateSummary(TraceState state) {
    if (state.entries().isEmpty()) {
      return "(없음)\n";
    }
    return state.entries().entrySet().stream()
        .map(entry -> "- " + entry.getKey() + ": " + describe(entry.getValue()))
        .collect(Collectors.joining("\n", "", "\n"));
  }

  private static String describe(TraceStateEntry entry) {
    if (entry.status() == TraceElementStatus.UNKNOWN) {
      return "UNKNOWN";
    }
    String content = entry.content();
    String shortened = content.length() > MAX_CONTENT_SUMMARY_LENGTH
        ? content.substring(0, MAX_CONTENT_SUMMARY_LENGTH) + "..." : content;
    return "FILLED (" + shortened + ")";
  }

  private static String missingSummary(TraceState state) {
    List<TraceElement> missing = state.missingElements();
    return missing.isEmpty() ? "(없음)\n" : missing.stream().map(Enum::name).collect(Collectors.joining(", ")) + "\n";
  }
}
