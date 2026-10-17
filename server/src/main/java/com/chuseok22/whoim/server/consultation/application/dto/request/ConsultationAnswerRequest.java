package com.chuseok22.whoim.server.consultation.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationAnswerRequest(
    @NotBlank(message = "질문은 비어 있을 수 없습니다.")
    @Size(max = 500, message = "질문은 500자 이하여야 합니다.")
    String question,
    @NotBlank(message = "답변은 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "답변은 1000자 이하여야 합니다.")
    String answer
) {

}
