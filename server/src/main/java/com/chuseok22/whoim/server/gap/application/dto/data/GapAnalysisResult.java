package com.chuseok22.whoim.server.gap.application.dto.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GapAnalysisResult(
    @NotNull(message = "gaps는 필수입니다.") @Valid List<GapCandidate> gaps
) {
}
