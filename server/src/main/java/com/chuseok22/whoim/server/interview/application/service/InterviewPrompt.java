package com.chuseok22.whoim.server.interview.application.service;

import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.interview.core.constant.TraceElement;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.core.constant.PromptCommonRules;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InterviewPrompt {

  private static final int MAX_CONTENT_SUMMARY_LENGTH = 80;

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FIRST_QUESTION_FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      - question에는 한국어 질문 하나만 작성합니다.
      - question은 비어 있을 수 없습니다.
      - 지정된 필드 외의 필드는 추가하지 않습니다.

      응답 형태:
      {"question":"당시 이 판단을 내릴 때 가장 중요하게 확인한 기준은 무엇이었나요?"}
      """;

  private static final String TURN_FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      - extracted에는 이번 턴에서 새롭게 확인하거나 명시적으로 정정한 TRACE 요소만 넣습니다.
      - element는 SITUATION, SIGNAL, CAUSE, CONDITION, JUDGMENT, ALTERNATIVE, ACTION, REASON, EXCEPTION, RISK 중 하나입니다.
      - status는 FILLED 또는 UNKNOWN입니다.
      - FILLED이면 content, source, excerpt를 모두 작성합니다. source는 DIRECT 또는 RECORDED입니다.
      - UNKNOWN이면 content, source, excerpt는 모두 null입니다.
      - DIRECT의 excerpt는 [전임자 답변]에서, RECORDED의 excerpt는 [기록 발췌]에서 실제 표현을 그대로 사용합니다.
      - 새롭게 추출할 요소가 없으면 extracted는 빈 배열입니다.
      - [남은 턴 수]가 1 이상이면 nextQuestion에 한국어 질문 하나를 반드시 작성합니다.
      - [남은 턴 수]가 0이면 nextQuestion은 null입니다.
      - TRACE 요소가 모두 채워졌다고 판단하더라도 [남은 턴 수]가 1 이상이면 nextQuestion을 생략하지 않습니다.
      - 지정된 필드 외의 필드는 추가하지 않습니다.

      응답 형태:
      {"extracted":[{"element":"JUDGMENT","status":"FILLED","content":"기존 방식보다 다른 처리 순서가 적절하다고 판단함","source":"DIRECT","excerpt":"이 경우에는 처리 순서를 바꾸는 게 맞다고 봤습니다."},{"element":"RISK","status":"UNKNOWN","content":null,"source":null,"excerpt":null}],"nextQuestion":"그 판단을 내릴 때 확인한 조건은 무엇이었나요?"}
      """;

  private InterviewPrompt() {
  }

  public static LlmRequest firstQuestion(String instruction, Gap gap, List<GapAnchor> anchors) {
    String prompt = "다음 Gap에 대한 첫 질문을 만드세요.\n\n" + PromptCommonRules.wrapInputData(gapSection(gap, anchors));
    return new LlmRequest(PromptCommonRules.assembleSystem(instruction, FIRST_QUESTION_FORMAT_BLOCK), prompt, null, null);
  }

  public static LlmRequest turn(
      String instruction, Gap gap, List<GapAnchor> anchors, TraceState state, String previousQuestion, String answer, int remainingTurns) {
    String data = gapSection(gap, anchors)
        + "\n[현재 TRACE 상태]\n" + stateSummary(state)
        + "\n[아직 확인되지 않은 요소]\n" + unseenSummary(state)
        + "\n[확인했지만 전임자가 알지 못한 요소]\n" + unknownSummary(state)
        + "\n[직전 질문]\n" + previousQuestion
        + "\n\n[전임자 답변]\n" + answer
        + "\n\n[남은 턴 수]\n" + remainingTurns;
    String prompt = "다음 입력 데이터를 바탕으로 TRACE 요소를 갱신하고 필요한 다음 질문 하나를 만드세요.\n\n"
        + PromptCommonRules.wrapInputData(data);
    return new LlmRequest(PromptCommonRules.assembleSystem(instruction, TURN_FORMAT_BLOCK), prompt, null, null);
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

  private static String unseenSummary(TraceState state) {
    return elementNames(Arrays.stream(TraceElement.values()).filter(element -> !state.entries().containsKey(element)));
  }

  private static String unknownSummary(TraceState state) {
    return elementNames(Arrays.stream(TraceElement.values()).filter(element -> {
      TraceStateEntry entry = state.entries().get(element);
      return entry != null && entry.status() == TraceElementStatus.UNKNOWN;
    }));
  }

  private static String elementNames(Stream<TraceElement> elements) {
    String names = elements.map(Enum::name).collect(Collectors.joining(", "));
    return names.isEmpty() ? "(없음)\n" : names + "\n";
  }
}
