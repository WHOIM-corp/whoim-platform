package com.chuseok22.whoim.server.interview.application.dto.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InterviewTurnResult(
    @NotNull(message = "추출 목록은 필수입니다.") @Valid List<ExtractedElement> extracted,
    String nextQuestion
) {

}
