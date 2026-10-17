package com.chuseok22.whoim.server.consultation.application.dto.data;

// 잘못된 항목은 서비스가 버린다
public record ConsultationSelection(
    String cardLabel,
    String explanation
) {

}
