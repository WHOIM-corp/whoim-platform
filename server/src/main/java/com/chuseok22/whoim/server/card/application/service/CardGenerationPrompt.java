package com.chuseok22.whoim.server.card.application.service;

import com.chuseok22.whoim.server.card.core.constant.EvidenceLinkType;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.interview.core.constant.TraceElementStatus;
import com.chuseok22.whoim.server.interview.core.constant.TraceState;
import com.chuseok22.whoim.server.interview.core.constant.TraceStateEntry;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CardGenerationPrompt {

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.

      응답 형태:
      {"title":"...","situation":"...","signal":null,"cause":"...","condition":null,"judgment":"...","alternative":null,"action":"...","reason":"...","exception":null,"risk":null,"directEvidence":[{"turnNumber":1,"excerpt":"..."}]}
      """;

  private CardGenerationPrompt() {
  }

  // 문서 전문·대화 전문은 보내지 않고, DIRECT 요소를 낸 턴의 답변만 발췌 후보로 보낸다
  public static LlmRequest build(String instruction, Gap gap, TraceState state, List<InterviewTurn> turns) {
    Set<Integer> directTurnNumbers = state.entries().values().stream()
        .filter(entry -> entry.status() == TraceElementStatus.FILLED && entry.source() == EvidenceLinkType.DIRECT)
        .map(TraceStateEntry::turnNumber)
        .collect(Collectors.toSet());
    String prompt = "다음 인터뷰 결과로 TRACE Card를 만드세요.\n\n"
        + "[Gap 요약]\n" + gap.getSummary()
        + "\n\n[질문]\n" + gap.getQuestion()
        + "\n\n[채워진 TRACE 요소]\n" + filledSection(state)
        + "\n[답변 발췌 후보]\n" + answerSection(turns, directTurnNumbers);
    return new LlmRequest(instruction + "\n\n" + FORMAT_BLOCK, prompt, null, null);
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
