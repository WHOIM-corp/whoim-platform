package com.chuseok22.whoim.server.llm.application.dto.data;

import java.util.function.UnaryOperator;

// restorer는 응답 content 안의 토큰을 JSON 문자열 이스케이프된 원문으로 되돌린다
public record GuardedInput(String maskedText, UnaryOperator<String> restorer) {
}
