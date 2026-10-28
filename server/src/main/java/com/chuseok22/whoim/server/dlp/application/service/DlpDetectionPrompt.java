package com.chuseok22.whoim.server.dlp.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;

public final class DlpDetectionPrompt {

  // 관리자가 지침을 어떻게 바꿔도 파서가 기대하는 출력 형식은 항상 뒤에 붙는다
  private static final String FORMAT_BLOCK = """
      - 반드시 JSON만 출력합니다. 설명, 코드 블록, 다른 문장은 쓰지 않습니다.
      - type은 CUSTOMER, PARTNER, PROJECT, PERSON, SECRET 중 하나입니다.
      - text에는 입력에 실제 등장한 민감한 고유명 또는 비밀값 자체만 씁니다. 조사, 직책·호칭, 앞뒤 설명을 포함하지 않습니다.
      - 이미 DLP가 발급한 마스킹 토큰 내부의 문자열은 탐지하지 않습니다.
      - `[C1]`, `[C2]` 같은 Card 또는 근거 라벨은 탐지하지 않습니다.
      - 입력에 없는 문자열을 만들거나 표기를 수정하지 않습니다.
      - 탐지할 정보가 없으면 detections는 빈 배열입니다.
      - 지정된 필드 외의 필드는 추가하지 않습니다.

      응답 형태:
      {"detections":[{"text":"가상엔티티명","type":"PROJECT"}]}

      탐지할 정보가 없는 경우:
      {"detections":[]}
      """;

  private DlpDetectionPrompt() {
  }

  public static LlmRequest build(String instruction, String maskedText) {
    return new LlmRequest(instruction + "\n\n" + FORMAT_BLOCK, maskedText, null, null);
  }
}
