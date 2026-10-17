package com.chuseok22.whoim.server.card.application.dto.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CardGenerationResult(
    @NotBlank(message = "제목은 비어 있을 수 없습니다.") String title,
    String situation,
    String signal,
    String cause,
    String condition,
    String judgment,
    String alternative,
    String action,
    String reason,
    String exception,
    String risk,
    @NotNull(message = "직접 근거 목록은 비어 있을 수 없습니다.") @Valid List<DirectEvidenceCandidate> directEvidence
) {

}
