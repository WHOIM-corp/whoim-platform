package com.chuseok22.whoim.server.consultation.application.dto.response;

import java.time.OffsetDateTime;

// 시각은 상태 이력에서 가져오므로 이력이 없으면 null일 수 있다
public record ConsultationVerificationResponse(
    OffsetDateTime predecessorConfirmedAt,
    OffsetDateTime approvedAt
) {

}
