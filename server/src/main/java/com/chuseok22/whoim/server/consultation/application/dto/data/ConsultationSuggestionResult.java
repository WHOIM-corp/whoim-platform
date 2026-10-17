package com.chuseok22.whoim.server.consultation.application.dto.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConsultationSuggestionResult(
    @NotNull(message = "선택 목록은 비어 있을 수 없습니다.") @Valid List<ConsultationSelection> selections
) {

}
