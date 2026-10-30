package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.core.constant.PromptCommonRules;

public final class GapAnalysisPrompt {

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      - gaps는 최대 3개입니다.
      - type은 CHOICE_GAP, EVIDENCE_CONFLICT, ASSESSMENT_GAP 중 하나입니다.
      - 입력의 문서는 `## D1 파일명 (유형)` 헤더로 시작하고 각 줄은 `[D1:L3] 내용` 형태입니다. anchors의 document에는 D1 같은 문서 라벨을, startLine과 endLine에는 L 뒤의 줄 번호를 사용합니다.
      - anchors의 document, startLine, endLine에는 입력에 실제 존재하는 값을 사용합니다.
      - CHOICE_GAP과 ASSESSMENT_GAP에는 유효한 anchor가 최소 1개 필요합니다.
      - EVIDENCE_CONFLICT에는 서로 다른 유효한 anchor가 최소 2개 필요합니다.
      - Gap이 없으면 gaps는 빈 배열입니다.
      - 지정된 필드 외의 필드는 추가하지 않습니다.

      응답 형태:
      {"gaps":[{"type":"CHOICE_GAP","summary":"특정 방식을 선택한 판단 기준이 기록에 없음","question":"당시 이 방식을 선택한 기준은 무엇이었나요?","anchors":[{"document":"D1","startLine":3,"endLine":4}]}]}
      """;

  private GapAnalysisPrompt() {
  }

  public static LlmRequest build(String instruction, String inputText) {
    return new LlmRequest(PromptCommonRules.assembleSystem(instruction, FORMAT_BLOCK),
        "다음 업무 기록에서 Gap을 찾으세요.\n\n" + PromptCommonRules.wrapInputData(inputText), null, null);
  }
}
