package com.chuseok22.whoim.server.interview.application.dto.data;

import jakarta.validation.constraints.NotBlank;

public record InterviewFirstQuestionResult(
    @NotBlank(message = "질문은 비어 있을 수 없습니다.") String question
) {

}
