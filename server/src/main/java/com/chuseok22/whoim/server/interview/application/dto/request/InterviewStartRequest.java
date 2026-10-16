package com.chuseok22.whoim.server.interview.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InterviewStartRequest(
    @NotNull(message = "Gap은 필수입니다.") UUID gapId
) {

}
