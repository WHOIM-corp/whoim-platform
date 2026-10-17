package com.chuseok22.whoim.server.dlp.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;

public final class DlpDetectionPrompt {

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      - type은 CUSTOMER, PARTNER, PROJECT, PERSON, SECRET 중 하나입니다.
      - 이미 `[…]` 형태의 토큰으로 가려진 것은 제외하고, 토큰은 다시 쓰지 않습니다.

      응답 형태:
      {"detections":[{"text":"...","type":"PERSON"}]}
      """;

  private DlpDetectionPrompt() {
  }

  public static LlmRequest build(String instruction, String maskedText) {
    return new LlmRequest(instruction + "\n\n" + FORMAT_BLOCK, maskedText, null, null);
  }
}
