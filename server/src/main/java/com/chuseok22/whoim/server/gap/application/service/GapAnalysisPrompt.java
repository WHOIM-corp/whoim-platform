package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;

public final class GapAnalysisPrompt {

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.

      응답 형태:
      {"gaps":[{"type":"CHOICE_GAP","summary":"...","question":"...","anchors":[{"document":"D1","startLine":3,"endLine":3}]}]}
      """;

  private GapAnalysisPrompt() {
  }

  public static LlmRequest build(String instruction, String inputText) {
    return new LlmRequest(instruction + "\n\n" + FORMAT_BLOCK, "다음 업무 기록에서 Gap을 찾으세요.\n\n" + inputText, null, null);
  }
}
