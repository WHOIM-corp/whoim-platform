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

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String JSON_ONLY_RULE = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.

      응답 형태:
      """;

  private static final String FIRST_QUESTION_FORMAT = JSON_ONLY_RULE + """
      {"question":"..."}
      """;

  private static final String TURN_FORMAT = JSON_ONLY_RULE + """
      {"extracted":[{"element":"CAUSE","status":"FILLED","content":"...","source":"DIRECT","excerpt":"..."}],"nextQuestion":"..."}
      """;

  private InterviewPrompt() {
  }

  public static LlmRequest firstQuestion(String instruction, Gap gap, List<GapAnchor> anchors) {
    String prompt = "다음 Gap에 대한 첫 질문을 만드세요.\n\n" + gapSection(gap, anchors);
    return new LlmRequest(instruction + "\n\n" + FIRST_QUESTION_FORMAT, prompt, null, null);
  }

  public static LlmRequest turn(
      String instruction, Gap gap, List<GapAnchor> anchors, TraceState state, String previousQuestion, String answer, int remainingTurns) {
    String prompt = "다음 답변에서 TRACE 요소를 추출하고 다음 질문을 만드세요.\n\n"
        + gapSection(gap, anchors)
        + "\n[현재 TRACE 상태]\n" + stateSummary(state)
        + "\n[비어 있는 요소]\n" + missingSummary(state)
        + "\n[직전 질문]\n" + previousQuestion
        + "\n\n[전임자 답변]\n" + answer
        + "\n\n[남은 턴 수]\n" + remainingTurns + "\n";
    return new LlmRequest(instruction + "\n\n" + TURN_FORMAT, prompt, null, null);
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
