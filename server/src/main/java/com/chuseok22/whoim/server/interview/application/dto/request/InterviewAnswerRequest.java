package com.chuseok22.whoim.server.interview.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InterviewAnswerRequest(
    @NotBlank(message = "답변은 비어 있을 수 없습니다.")
    @Size(max = InterviewAnswerRequest.MAX_ANSWER_LENGTH, message = "답변은 4000자 이하여야 합니다.")
    String answer
) {

  public static final int MAX_ANSWER_LENGTH = 4000;
}
