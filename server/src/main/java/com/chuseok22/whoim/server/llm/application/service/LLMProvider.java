package com.chuseok22.whoim.server.llm.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.LlmRequest;
import com.chuseok22.whoim.server.llm.application.dto.data.LlmResponse;

// 교체 가능한 외부 연동이라 인터페이스를 둔다. 실패 시 CustomException(LLM_UNAVAILABLE)을 던진다
public interface LLMProvider {

  LlmResponse generate(LlmRequest request);
}
