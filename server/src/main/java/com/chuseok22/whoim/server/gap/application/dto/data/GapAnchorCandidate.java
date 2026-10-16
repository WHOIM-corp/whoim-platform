package com.chuseok22.whoim.server.gap.application.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GapAnchorCandidate(
    @NotBlank(message = "document는 필수입니다.") String document,
    @NotNull(message = "startLine은 필수입니다.") Integer startLine,
    @NotNull(message = "endLine은 필수입니다.") Integer endLine
) {
}
