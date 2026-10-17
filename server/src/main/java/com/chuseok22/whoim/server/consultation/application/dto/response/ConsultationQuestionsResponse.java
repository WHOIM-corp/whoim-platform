package com.chuseok22.whoim.server.consultation.application.dto.response;

import java.util.List;

// 후보 Card가 없으면 questions는 비고 message에 안내 문구가 담긴다
public record ConsultationQuestionsResponse(
    List<String> questions,
    String message
) {

}
