package com.chuseok22.whoim.server.consultation.application.dto.response;

import java.util.List;

// 제시할 Card가 없으면 cards는 비고 message에 안내 문구가 담긴다
public record ConsultationSuggestionsResponse(
    List<ConsultationCardResponse> cards,
    String message
) {

}
