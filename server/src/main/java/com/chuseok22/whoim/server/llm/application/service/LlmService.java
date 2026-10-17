package com.chuseok22.whoim.server.llm.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.GuardedInput;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

  private final LlmChain chain;
  private final LlmInputGuard guard;

  // Lombok 설정에 @Qualifier 복사가 없어 같은 타입 빈과 구분하도록 생성자를 직접 쓴다
  public LlmService(@Qualifier("generationChain") LlmChain chain, LlmInputGuard guard) {
    this.chain = chain;
    this.guard = guard;
  }

  // 외부로 나가는 prompt는 항상 마스킹하고, 응답 content의 토큰은 파싱 전에 원문으로 복원한다
  public <T> T generate(LlmRequest request, Class<T> responseType) {
    GuardedInput guarded = guard.guard(request.prompt());
    LlmRequest maskedRequest = new LlmRequest(
        request.system(), guarded.maskedText(), request.responseSchema(), request.temperature());
    return chain.generate(maskedRequest, responseType, guarded.restorer());
  }
}
