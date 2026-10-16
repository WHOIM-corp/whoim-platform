package com.chuseok22.whoim.server.llm.application.dto.data;

public record LlmResponse(
    String content,
    String providerName
) {
}
