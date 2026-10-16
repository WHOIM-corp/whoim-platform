package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;

public final class GapAnalysisPrompt {

  private static final String SYSTEM = """
      당신은 업무 기록을 읽고 전임자에게 확인해야 할 판단의 빈틈(Gap)을 찾는 분석가입니다.
      아래 세 가지 유형만 찾습니다.
      - CHOICE_GAP: 여러 대안 중 하나를 골랐는데 왜 그것을 골랐는지 기록에 없는 경우
      - EVIDENCE_CONFLICT: 기록끼리 서로 다른 말을 하거나 결과가 어긋나는 경우(서로 다른 두 곳 이상의 근거가 필요)
      - ASSESSMENT_GAP: 상황을 어떻게 판단했는지, 그 판단 기준이 기록에 없는 경우

      규칙:
      - 입력의 각 줄은 `[D1:L3] 내용` 형태입니다. D1은 문서, L3은 줄 번호입니다. 근거의 줄 번호는 입력에 나온 것만 쓰고, 없는 번호를 만들지 않습니다.
      - 근거는 주어진 줄 번호 안에서만 고릅니다. 기록에 없는 사실은 추측하지 않습니다.
      - 최대 3개까지만 찾습니다. 근거가 부족하면 gaps를 빈 배열로 둡니다.
      - summary는 빈틈 요약, question은 전임자에게 물어볼 질문 후보입니다. 한국어로 씁니다.
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.

      응답 형태:
      {"gaps":[{"type":"CHOICE_GAP","summary":"...","question":"...","anchors":[{"document":"D1","startLine":3,"endLine":3}]}]}
      """;

  private GapAnalysisPrompt() {
  }

  public static LlmRequest build(String inputText) {
    return new LlmRequest(SYSTEM, "다음 업무 기록에서 Gap을 찾으세요.\n\n" + inputText, null, null);
  }
}
