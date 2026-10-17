package com.chuseok22.whoim.server.consultation.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationQuestionsRequest(
    @NotBlank(message = "상황은 비어 있을 수 없습니다.")
    @Size(max = 2000, message = "상황은 2000자 이하여야 합니다.")
    String situation
) {

}
