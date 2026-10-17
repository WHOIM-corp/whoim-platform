package com.chuseok22.whoim.server.gap.application.dto.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// type은 문자열로 받는다. 알 수 없는 유형이 전체 파싱을 깨지 않게 하기 위해서다
public record GapCandidate(
    @NotBlank(message = "type은 필수입니다.") String type,
    @NotBlank(message = "summary는 필수입니다.") String summary,
    @NotBlank(message = "question은 필수입니다.") String question,
    @NotNull(message = "anchors는 필수입니다.") @Valid List<GapAnchorCandidate> anchors
) {
}
