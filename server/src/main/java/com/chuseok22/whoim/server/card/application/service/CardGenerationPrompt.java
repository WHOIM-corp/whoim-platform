package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.core.constant.PromptCommonRules;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CardGenerationPrompt {

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      - title은 비어 있을 수 없습니다.
      - TRACE 10개 필드는 대응하는 요소가 FILLED이면 문자열, 아니면 null입니다.
      - directEvidence에는 [답변 발췌 후보]에 실제 존재하는 구절만 사용합니다.
      - turnNumber에는 해당 답변의 실제 턴 번호를 사용합니다.
      - directEvidence에는 유효한 항목을 최소 1개 포함합니다.
      - excerpt는 답변 원문의 표현을 그대로 사용합니다.
      - 지정된 필드 외의 필드는 추가하지 않습니다.

      응답 형태:
      {"title":"특정 조건에서 처리 순서를 변경한 판단","situation":"기존 절차를 그대로 적용하기 어려운 상황","signal":null,"cause":null,"condition":"특정 조건이 확인된 경우","judgment":"처리 순서를 변경하는 것이 적절하다고 판단함","alternative":"기존 순서를 유지하는 방안","action":"처리 순서를 변경함","reason":"현재 조건에 기존 순서가 적합하지 않다고 판단했기 때문","exception":null,"risk":null,"directEvidence":[{"turnNumber":2,"excerpt":"이 조건에서는 기존 순서로 처리하기 어렵다고 판단했습니다."}]}
      """;

  private CardGenerationPrompt() {
  }

  // 문서 전문·대화 전문은 보내지 않고, DIRECT 요소를 낸 턴의 답변만 발췌 후보로 보낸다
  public static LlmRequest build(String instruction, Gap gap, TraceState state, List<InterviewTurn> turns) {
    Set<Integer> directTurnNumbers = state.entries().values().stream()
        .filter(entry -> entry.status() == TraceElementStatus.FILLED && entry.source() == EvidenceLinkType.DIRECT)
        .map(TraceStateEntry::turnNumber)
        .collect(Collectors.toSet());
    String sections = "[Gap 요약]\n" + gap.getSummary()
        + "\n\n[질문]\n" + gap.getQuestion()
        + "\n\n[채워진 TRACE 요소]\n" + filledSection(state)
        + "\n[답변 발췌 후보]\n" + answerSection(turns, directTurnNumbers);
    String prompt = "다음 인터뷰 결과로 TRACE Card를 만드세요.\n\n" + PromptCommonRules.wrapInputData(sections.stripTrailing());
    return new LlmRequest(PromptCommonRules.assembleSystem(instruction, FORMAT_BLOCK), prompt, null, null);
  }

  private static String filledSection(TraceState state) {
    return state.entries().entrySet().stream()
        .filter(entry -> entry.getValue().status() == TraceElementStatus.FILLED)
        .map(entry -> "- " + entry.getKey() + ": " + entry.getValue().content())
        .collect(Collectors.joining("\n", "", "\n"));
  }

  private static String answerSection(List<InterviewTurn> turns, Set<Integer> directTurnNumbers) {
    return turns.stream()
        .filter(turn -> turn.getAnswer() != null && directTurnNumbers.contains(turn.getTurnNumber()))
        .map(turn -> "- 턴 " + turn.getTurnNumber() + ": " + turn.getAnswer())
        .collect(Collectors.joining("\n", "", "\n"));
  }
}
