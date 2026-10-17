package com.chuseok22.whoim.server.card.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardRejectRequest(
    @NotBlank(message = "거절 사유는 비어 있을 수 없습니다.")
    @Size(max = 500, message = "거절 사유는 500자 이하여야 합니다.")
    String reason
) {

}
