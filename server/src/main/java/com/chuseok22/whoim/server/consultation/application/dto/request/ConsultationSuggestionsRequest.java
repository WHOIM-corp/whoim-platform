package com.chuseok22.whoim.server.consultation.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// answers가 null이면 추가 질문 없이 바로 제시하는 것으로 본다
public record ConsultationSuggestionsRequest(
    @NotBlank(message = "상황은 비어 있을 수 없습니다.")
    @Size(max = 2000, message = "상황은 2000자 이하여야 합니다.")
    String situation,
    @Size(max = 3, message = "답변은 3개 이하여야 합니다.")
    List<@NotNull(message = "답변 항목은 비어 있을 수 없습니다.") @Valid ConsultationAnswerRequest> answers
) {

}
