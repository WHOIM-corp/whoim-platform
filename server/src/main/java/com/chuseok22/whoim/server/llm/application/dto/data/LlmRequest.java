package com.chuseok22.whoim.server.llm.application.dto.data;

import java.util.Map;

// system, responseSchema, temperature는 null이면 Provider 기본값을 따른다
public record LlmRequest(
    String system,
    String prompt,
    Map<String, Object> responseSchema,
    Double temperature
) {
}
