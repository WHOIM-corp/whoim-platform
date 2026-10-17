package com.chuseok22.whoim.server.consultation.application.dto.data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// 요소 단위 검증은 두지 않는다. 빈 질문은 서비스가 걸러내 한 항목 때문에 전체가 503이 되지 않게 한다
public record ConsultationQuestionsResult(
    @NotNull(message = "질문 목록은 비어 있을 수 없습니다.") List<String> questions
) {

}
