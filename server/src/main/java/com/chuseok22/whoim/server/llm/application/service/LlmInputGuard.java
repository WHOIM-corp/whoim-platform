package com.chuseok22.whoim.server.llm.application.service;

import com.chuseok22.whoim.server.llm.application.dto.data.GuardedInput;

// llm이 dlp에 의존하지 않도록 인터페이스만 둔다. 검사를 끝내지 못하면 CustomException(DLP_UNAVAILABLE)을 던진다
public interface LlmInputGuard {

  GuardedInput guard(String text);
}
